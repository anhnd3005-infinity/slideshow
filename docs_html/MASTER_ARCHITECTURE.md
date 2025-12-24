# 🎬 Master Architecture Document - Video Editor Kiểu CapCut

## 📚 Tổng Quan Documents

1. **[ARCHITECTURE_CAPCUT_STYLE.md](./ARCHITECTURE_CAPCUT_STYLE.md)** - Kiến trúc chi tiết multi-track timeline
2. **[ARCHITECTURE_HUONG2.md](./ARCHITECTURE_HUONG2.md)** - Kiến trúc ExoPlayer + GL compositing
3. **[REALTIME_PREVIEW_EXPLAINED.md](./REALTIME_PREVIEW_EXPLAINED.md)** - Giải thích real-time preview
4. **[TIMELINE_LAYERS_EXPLAINED.md](./TIMELINE_LAYERS_EXPLAINED.md)** - Giải thích timeline layers theo thời gian

---

## 🎯 Tổng Quan Hệ Thống

Hệ thống video editor đa lớp với timeline multi-track, hỗ trợ:
- ✅ **Mix ảnh + video** trong cùng timeline
- ✅ **Multi-track**: Video, Overlay, Audio, Text tracks
- ✅ **Keyframe animation**: Transform, opacity, effects theo thời gian
- ✅ **Rich effects**: Filter, transition, sticker, text với animation
- ✅ **Audio mixing**: Background music, sound effects, voice-over
- ✅ **Real-time preview**: Smooth playback với effects (30-60fps)
- ✅ **Timeline layers**: Mỗi layer có startTime/duration riêng
- ✅ **High-quality export**: Multi-pass rendering với MediaCodec

---

## 🔄 Luồng Hoàn Chỉnh: Từ Chọn Media → Preview → Export

### Phase 1: User Chọn Ảnh + Video

#### 1.1. MainActivity - User Click "Create Slideshow"

```kotlin
// MainActivity.kt
buttonStart.setOnClickListener {
    viewModel?.startImagePicker()
}
```

#### 1.2. MainViewModel - Launch Image Picker

```kotlin
// MainViewModel.kt
fun startImagePicker() {
    val currentActivity = activity ?: return
    
    val mediaItems = ArrayList<ImageModel>()
    
    // Bước 1: Chọn ảnh (optional, min = 0)
    TedImagePicker.with(currentActivity)
        .image()
        .min(0, "Bạn có thể bỏ qua chọn ảnh")
        .max(50, "Bạn chỉ có thể chọn tối đa 50 ảnh")
        .startMultiImage { imageUris ->
            // Lưu ảnh đã chọn
            imageUris.forEach { uri ->
                mediaItems.add(ImageModel(uri, isVideo = false))
            }
            
            // Bước 2: Chọn video (optional, min = 0)
            TedImagePicker.with(currentActivity)
                .video()
                .min(0, "Bạn có thể bỏ qua chọn video")
                .max(20, "Bạn chỉ có thể chọn tối đa 20 video")
                .startMultiImage { videoUris ->
                    // Lưu video đã chọn
                    videoUris.forEach { uri ->
                        mediaItems.add(ImageModel(uri, isVideo = true))
                    }
                    
                    // Kiểm tra có media nào không
                    if (mediaItems.isEmpty()) {
                        Toast.makeText(currentActivity, "Vui lòng chọn ít nhất 1 ảnh hoặc video", Toast.LENGTH_SHORT).show()
                        return@startMultiImage
                    }
                    
                    // Bước 3: Start SlideShowActivity với danh sách media
                    val intent = Intent(currentActivity, SlideShowActivity::class.java)
                    intent.putParcelableArrayListExtra(
                        Constant.EXTRA_ARRAY_IMAGE,
                        mediaItems
                    )
                    currentActivity.startActivity(intent)
                }
        }
}
```

**Flow:**
```
User click "Create Slideshow"
    ↓
MainViewModel.startImagePicker()
    ↓
TedImagePicker (Image) → User chọn ảnh
    ↓
TedImagePicker (Video) → User chọn video
    ↓
Tạo ArrayList<ImageModel> với isVideo flag
    ↓
Start SlideShowActivity với intent
```

---

### Phase 2: SlideShowActivity - Initialize Timeline

#### 2.1. SlideShowActivity.onCreate()

```kotlin
// SlideShowActivity.kt
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Check audio permission
    checkAndRequestAudioPermission()
    
    // Initialize view
    initView()
    initEvent()
}

private fun initView() {
    // Get media list from intent
    val imageList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableArrayListExtra(Constant.EXTRA_ARRAY_IMAGE, ImageModel::class.java)
    } else {
        @Suppress("DEPRECATION")
        intent.getParcelableArrayListExtra<ImageModel>(Constant.EXTRA_ARRAY_IMAGE)
    }
    
    // Setup ViewModel
    viewModel = ViewModelProviders.of(this).get(SlideShowViewModel::class.java)
    viewModel.setBinding(binding)
    viewModel.initDataBase(this)
    
    // Load media vào timeline
    viewModel.loadDataImage(imageList)
    
    // Load other resources
    viewModel.loadTextQuote()
    viewModel.loadDataMusic()
}
```

#### 2.2. SlideShowViewModel.loadDataImage()

```kotlin
// SlideShowViewModel.kt
fun loadDataImage(listImage: ArrayList<ImageModel>?) {
    if (listImage == null || listImage.isEmpty()) {
        Toast.makeText(context, "No media selected", Toast.LENGTH_SHORT).show()
        return
    }
    
    // Convert ImageModel → MediaScene → Clip
    applyData(listImage)
}

private fun applyData(items: List<ImageModel>) {
    Single.just(items)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .map { inputItems ->
            inputItems.mapIndexed { index, model ->
                createClipFromMedia(model, index)
            }
        }
        .subscribeBy { clips ->
            // Add clips vào timeline
            clips.forEach { clip ->
                addClipToTimeline(clip)
            }
            
            // Start preview
            startPreview()
        }
        .willBeDisposed()
}

private fun createClipFromMedia(model: ImageModel, index: Int): Clip {
    return when {
        model.isVideo -> {
            // Video clip
            Clip(
                id = UUID.randomUUID().toString(),
                type = ClipType.VIDEO,
                source = MediaSource(model.uriImage),
                startTime = calculateStartTime(index),  // Dựa vào clips trước đó
                duration = getVideoDuration(model.uriImage),  // Từ MediaMetadataRetriever
                speed = 1.0f
            )
        }
        else -> {
            // Image clip
            Clip(
                id = UUID.randomUUID().toString(),
                type = ClipType.IMAGE,
                source = MediaSource(model.uriImage),
                startTime = calculateStartTime(index),
                duration = 3000L,  // Default 3s cho ảnh
                speed = 1.0f
            )
        }
    }
}

private fun addClipToTimeline(clip: Clip) {
    val track = when (clip.type) {
        ClipType.VIDEO, ClipType.IMAGE -> {
            timelineController.getTrack(TrackType.VIDEO) 
                ?: timelineController.addTrack(TrackType.VIDEO)
        }
        ClipType.AUDIO -> {
            timelineController.getTrack(TrackType.AUDIO)
                ?: timelineController.addTrack(TrackType.AUDIO)
        }
        // ... other types
    }
    
    track.addClip(clip)
    
    // Setup media source (ExoPlayer cho video, Bitmap cho image)
    when (clip.type) {
        ClipType.VIDEO -> mediaSourceManager.setupVideoClip(clip)
        ClipType.IMAGE -> mediaSourceManager.loadImage(clip.source)
        // ...
    }
}
```

**Flow:**
```
SlideShowActivity.onCreate()
    ↓
Get ArrayList<ImageModel> from intent
    ↓
SlideShowViewModel.loadDataImage()
    ↓
For each ImageModel:
    ├─ isVideo = true → Create Video Clip
    └─ isVideo = false → Create Image Clip
    ↓
Add clips to TimelineController
    ↓
Setup media sources:
    ├─ Video → ExoPlayer + SurfaceTexture
    └─ Image → Load Bitmap
    ↓
Timeline ready!
```

---

### Phase 3: Setup Rendering Pipeline

#### 3.1. Initialize Compositor

```kotlin
// SlideShowViewModel.kt
fun initDataBase(context: SlideShowActivity) {
    this.context = context
    
    // Initialize compositor
    mediaCompositor = MultiLayerCompositor(
        context = context,
        glContext = eglContext,
        timelineController = timelineController,
        mediaSourceManager = mediaSourceManager
    )
    
    // Setup TextureView for preview
    setupTextureView()
    
    // Initialize other components
    initViewMenuBar()
    initTransitions()
    initFilters()
    // ...
}

private fun setupTextureView() {
    binding.textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            // Initialize GL context
            val eglContext = EGL14.eglGetCurrentContext()
            
            // Setup compositor với surface
            mediaCompositor.setupSurface(surface, width, height)
            
            // Start preview render loop
            startPreview()
        }
        
        // ... other callbacks
    }
}
```

#### 3.2. Start Preview Render Loop

```kotlin
// SlideShowViewModel.kt
private fun startPreview() {
    previewRenderer = PreviewRenderer(
        compositor = mediaCompositor,
        timelineController = timelineController,
        textureView = binding.textureView
    )
    
    previewRenderer.start()
}

// PreviewRenderer.kt
class PreviewRenderer(
    private val compositor: MultiLayerCompositor,
    private val timelineController: TimelineController,
    private val textureView: TextureView
) {
    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = false
    private var currentTime = 0L
    private val fps = 30
    private val frameInterval = 1000L / fps  // 33ms
    
    fun start() {
        isPlaying = true
        renderLoop()
    }
    
    fun pause() {
        isPlaying = false
    }
    
    fun seekTo(time: Long) {
        currentTime = time
        renderFrame(time)  // Render ngay frame này
    }
    
    private fun renderLoop() {
        if (!isPlaying) return
        
        // Render frame tại thời điểm hiện tại
        renderFrame(currentTime)
        
        // Update time
        currentTime += frameInterval
        
        // Loop back nếu hết timeline
        if (currentTime >= timelineController.totalDuration) {
            currentTime = 0L
        }
        
        // Schedule next frame
        handler.postDelayed({ renderLoop() }, frameInterval)
    }
    
    private fun renderFrame(time: Long) {
        // Query clips tại thời điểm này
        val clipsAtTime = timelineController.getClipsAtTime(time)
        
        // Render với compositor
        val outputTexture = compositor.renderFrame(time, clipsAtTime)
        
        // Update TextureView
        textureView.setSurfaceTexture(outputTexture)
    }
}
```

**Flow:**
```
SlideShowViewModel.initDataBase()
    ↓
Create MultiLayerCompositor
    ↓
Setup TextureView
    ↓
onSurfaceTextureAvailable()
    ↓
Initialize GL context
    ↓
Start PreviewRenderer
    ↓
Render loop chạy liên tục (30fps)
    ↓
Mỗi frame:
    ├─ Query clips tại thời điểm hiện tại
    ├─ Render với compositor
    └─ Update TextureView
```

---

### Phase 4: Render Frame (Chi Tiết)

#### 4.1. MultiLayerCompositor.renderFrame()

```kotlin
// MultiLayerCompositor.kt
fun renderFrame(time: Long, clipsAtTime: Map<TrackType, List<Clip>>): Texture2d {
    // === STEP 1: Render Video/Image Track (Base Layer) ===
    val videoClips = clipsAtTime[TrackType.VIDEO] ?: emptyList()
    val baseTexture = renderVideoTrack(videoClips, time)
    
    // === STEP 2: Apply Filters ===
    val filteredTexture = applyFilters(baseTexture, videoClips, time)
    
    // === STEP 3: Apply Transforms ===
    val transformedTexture = applyTransforms(filteredTexture, videoClips, time)
    
    // === STEP 4: Render Overlay Track ===
    val overlayClips = clipsAtTime[TrackType.OVERLAY] ?: emptyList()
    val withOverlayTexture = renderOverlayTrack(transformedTexture, overlayClips, time)
    
    // === STEP 5: Render Text Track ===
    val textClips = clipsAtTime[TrackType.TEXT] ?: emptyList()
    val withTextTexture = renderTextTrack(withOverlayTexture, textClips, time)
    
    // === STEP 6: Mix Audio ===
    val audioClips = clipsAtTime[TrackType.AUDIO] ?: emptyList()
    audioMixer.mixAudio(audioClips, time)
    
    return withTextTexture
}

private fun renderVideoTrack(clips: List<Clip>, time: Long): Texture2d {
    if (clips.isEmpty()) return emptyTexture
    
    val clip = clips.first()  // Lấy clip đầu tiên (có thể có nhiều clip overlap)
    
    return when (clip.type) {
        ClipType.VIDEO -> {
            // Update ExoPlayer texture
            mediaSourceManager.updateVideoTexture(clip, time)
            mediaSourceManager.getVideoTexture(clip.id)
        }
        ClipType.IMAGE -> {
            // Load image texture
            val bitmap = mediaSourceManager.loadImage(clip.source)
            bitmapToTexture(bitmap)
        }
        else -> emptyTexture
    }
}

private fun applyFilters(texture: Texture2d, clips: List<Clip>, time: Long): Texture2d {
    val clip = clips.firstOrNull() ?: return texture
    val filter = clip.filter ?: return texture
    
    filterShader.use {
        filterShader.setTexture(texture)
        filterShader.setFilter(filter)
        return filterShader.render()
    }
}

private fun applyTransforms(texture: Texture2d, clips: List<Clip>, time: Long): Texture2d {
    val clip = clips.firstOrNull() ?: return texture
    
    // Calculate transform với keyframes
    val transform = calculateTransformAtTime(clip, time)
    
    transformShader.use {
        transformShader.setTexture(texture)
        transformShader.setTransform(transform)
        transformShader.setOpacity(transform.opacity)
        return transformShader.render()
    }
}

private fun renderOverlayTrack(baseTexture: Texture2d, overlayClips: List<Clip>, time: Long): Texture2d {
    var resultTexture = baseTexture
    
    // Sort by z-index
    overlayClips.sortedBy { it.transform.zIndex }.forEach { overlayClip ->
        val overlayTexture = getTextureForClip(overlayClip, time)
        
        blendShader.use {
            blendShader.setSourceTexture(overlayTexture)
            blendShader.setDestinationTexture(resultTexture)
            blendShader.setBlendMode(BlendMode.NORMAL)
            blendShader.setOpacity(overlayClip.transform.opacity)
            resultTexture = blendShader.render()
        }
    }
    
    return resultTexture
}

private fun renderTextTrack(baseTexture: Texture2d, textClips: List<Clip>, time: Long): Texture2d {
    var resultTexture = baseTexture
    
    textClips.forEach { textClip ->
        // Render text thành bitmap/texture
        val textTexture = renderTextToTexture(textClip, time)
        
        blendShader.use {
            blendShader.setSourceTexture(textTexture)
            blendShader.setDestinationTexture(resultTexture)
            resultTexture = blendShader.render()
        }
    }
    
    return resultTexture
}
```

**Flow chi tiết mỗi frame:**
```
Render Frame tại time = T
    ↓
Query clips tại T:
    ├─ VIDEO track: [clip1 (0-10s)] ✅
    ├─ AUDIO track: [music1 (0-10s)] ✅
    ├─ OVERLAY track: [frame1 (5-20s)] ✅
    └─ TEXT track: [text1 (8-12s)] ✅
    ↓
STEP 1: Render Video Track
    ├─ clip1 là VIDEO → Update ExoPlayer texture
    └─ Get video texture
    ↓
STEP 2: Apply Filter
    ├─ clip1.filter = Vivid
    └─ Apply filter shader
    ↓
STEP 3: Apply Transform
    ├─ Calculate transform với keyframes
    └─ Apply transform shader
    ↓
STEP 4: Render Overlay
    ├─ frame1 (5-20s) đang active
    └─ Blend overlay texture
    ↓
STEP 5: Render Text
    ├─ text1 (8-12s) đang active
    └─ Blend text texture
    ↓
STEP 6: Mix Audio
    ├─ music1 (0-10s) đang active
    └─ Play audio
    ↓
Output texture → TextureView
```

---

### Phase 5: User Apply Effects (Real-time Preview)

#### 5.1. User Click "Apply Filter"

```kotlin
// SlideShowViewModel.kt
fun applyFilterToClip(clipId: String, filterId: String) {
    // Tìm clip trong timeline
    val clip = timelineController.findClip(clipId) ?: return
    
    // Update filter property
    clip.filter = FilterLibrary.getFilter(filterId)
    
    // ✅ Preview ngay lập tức!
    // Render loop sẽ tự động pick up change ở frame tiếp theo
    // Không cần gọi render() thủ công
}
```

#### 5.2. User Add Overlay Layer

```kotlin
// SlideShowViewModel.kt
fun addOverlayLayer(overlayUri: Uri, startTime: Long, duration: Long) {
    val overlayClip = Clip(
        type = ClipType.EFFECT_OVERLAY,
        source = MediaSource(overlayUri),
        startTime = startTime,
        duration = duration
    )
    
    // Load texture
    overlayClip.texture = loadOverlayTexture(overlayUri)
    
    // Add to overlay track
    timelineController.getTrack(TrackType.OVERLAY)?.addClip(overlayClip)
    
    // ✅ Preview ngay! Overlay sẽ xuất hiện ở frame tiếp theo
}
```

#### 5.3. User Add Audio Layer

```kotlin
// SlideShowViewModel.kt
fun addAudioLayer(audioUri: Uri, startTime: Long, duration: Long) {
    val audioClip = Clip(
        type = ClipType.AUDIO,
        source = MediaSource(audioUri),
        startTime = startTime,
        duration = duration,
        volume = 0.8f
    )
    
    // Setup ExoPlayer cho audio
    mediaSourceManager.setupAudioClip(audioClip)
    
    // Add to audio track
    timelineController.getTrack(TrackType.AUDIO)?.addClip(audioClip)
    
    // ✅ Preview ngay! Audio sẽ play ở frame tiếp theo
}
```

**Flow khi apply effect:**
```
User action (click button, drag, etc.)
    ↓
Update clip property (filter, transform, etc.)
    ↓
Clip data updated (chỉ data, không render)
    ↓
Render loop (đang chạy):
    Frame N: Render với data cũ
    Frame N+1 (33ms sau):
        → Query clips → Đọc property mới
        → Render với effect mới
    ↓
Screen: Preview ngay lập tức! ✅
```

---

### Phase 6: Export Video

#### 6.1. User Click "Export"

```kotlin
// SlideShowViewModel.kt
fun exportVideo() {
    SaveVideoBottomSheet
        .newInstance()
        .show(context.supportFragmentManager, "save_video")
}

// SaveVideoBottomSheet callback
fun onExportConfirmed(width: Int, height: Int, quality: ExportQuality) {
    viewModel.exportVideo(width, height, quality)
}

fun exportVideo(width: Int, height: Int, quality: ExportQuality) {
    isExport.value = true
    
    // Setup exporter
    val exporter = VideoExporter(
        context = context,
        timelineController = timelineController,
        compositor = mediaCompositor,
        audioMixer = audioMixer
    )
    
    // Export với progress callback
    exporter.export(
        outputPath = getOutputPath(),
        width = width,
        height = height,
        fps = 30,
        bitrate = quality.bitrate,
        onProgress = { progress ->
            // Update UI
            binding.progressBarExport.progress = (progress * 100).toInt()
            binding.textViewMessageExport.text = "Exporting (${(progress * 100).toInt()}%)"
        }
    ) { outputPath ->
        // Export completed
        isExport.value = false
        Toast.makeText(context, "Video exported: $outputPath", Toast.LENGTH_SHORT).show()
    }
}
```

#### 6.2. VideoExporter.export()

```kotlin
// VideoExporter.kt
fun export(
    outputPath: String,
    width: Int,
    height: Int,
    fps: Int,
    bitrate: Int,
    onProgress: (Float) -> Unit,
    onComplete: (String) -> Unit
) {
    val totalDuration = timelineController.totalDuration
    val frameCount = (totalDuration / 1000f * fps).toInt()
    
    // Setup encoders
    val videoEncoder = setupVideoEncoder(width, height, fps, bitrate)
    val audioEncoder = setupAudioEncoder()
    val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MP4_2)
    
    var videoTrackIndex = -1
    var audioTrackIndex = -1
    
    // Render and encode each frame
    for (frameIndex in 0 until frameCount) {
        val time = (frameIndex * 1000L / fps)
        val progress = frameIndex.toFloat() / frameCount
        onProgress(progress)
        
        // === Render frame (giống preview nhưng full quality) ===
        val clipsAtTime = timelineController.getClipsAtTime(time)
        val frameTexture = compositor.renderFrame(time, clipsAtTime, quality = 1.0f)
        
        // === Encode video frame ===
        encodeVideoFrame(videoEncoder, frameTexture, frameIndex, fps)
        
        // === Encode audio (mỗi giây một lần) ===
        if (frameIndex % fps == 0) {
            val audioBuffer = audioMixer.prepareAudio(time, 1000L)
            encodeAudioFrame(audioEncoder, audioBuffer)
        }
    }
    
    // Finish encoding
    finishEncoding(videoEncoder, audioEncoder, muxer)
    
    onComplete(outputPath)
}
```

**Flow export:**
```
User click "Export"
    ↓
Show quality options (720p, 1080p, 4K)
    ↓
User chọn quality
    ↓
VideoExporter.export()
    ↓
Setup MediaCodec encoders:
    ├─ Video encoder (H.264)
    └─ Audio encoder (AAC)
    ↓
For each frame (0 → totalDuration):
    ├─ Render frame với full quality
    ├─ Encode video frame
    └─ Encode audio (mỗi giây)
    ↓
MediaMuxer combine video + audio
    ↓
Save MP4 file
    ↓
Show completion message
```

---

## 📊 Timeline Example: Chi Tiết

### Scenario: Video 30s với nhiều layers

```
Time:    0s    5s    10s   15s   20s   25s   30s
        |-----|-----|-----|-----|-----|-----|
VIDEO:  [====================================]
        Main video clip (0-30s)
        |-----|-----|-----|-----|-----|-----|
AUDIO:  [========]     [====================]
        Music 1 (0-10s)  Music 2 (15-30s)
        [==] (sfx 5-7s)
        |-----|-----|-----|-----|-----|-----|
OVERLAY:     [===============]
             Frame overlay (5-20s)
             [=======================]
             Vignette (10-25s)
        |-----|-----|-----|-----|-----|-----|
TEXT:   [====]
        Title (0-5s)
        [====]
        Subtitle (8-12s)
        |-----|-----|-----|-----|-----|-----|
STICKER:            [==========]
                    Sticker 1 (15-25s)
                    [========]
                    Sticker 2 (20-28s)
```

### Render tại các thời điểm:

**Tại 0s:**
- ✅ Video clip
- ✅ Music 1
- ✅ Title text

**Tại 7s:**
- ✅ Video clip
- ✅ Music 1
- ✅ Frame overlay
- ✅ Subtitle text

**Tại 18s:**
- ✅ Video clip
- ✅ Music 2
- ✅ Frame overlay
- ✅ Vignette
- ✅ Sticker 1

**Tại 25s:**
- ✅ Video clip
- ✅ Music 2
- ✅ Sticker 2

---

## 🎯 Key Components Summary

### 1. TimelineController
- Quản lý multi-track timeline
- Query clips tại thời điểm cụ thể
- Tính toán duration, progress

### 2. MediaSourceManager
- Quản lý ExoPlayer pool cho video
- Load và cache images
- Setup audio players

### 3. MultiLayerCompositor
- Render pipeline chính
- Apply filters, transforms, overlays
- Output texture cho preview/export

### 4. PreviewRenderer
- Render loop (30-60fps)
- Sync với timeline
- Update TextureView

### 5. VideoExporter
- Encode frames với MediaCodec
- Mix audio
- Mux thành MP4

---

## ✅ Checklist Implementation

### Phase 1: Core Timeline ✅
- [x] TimelineController
- [x] Track & Clip data structures
- [x] Query clips tại thời điểm

### Phase 2: Media Playback ✅
- [x] ExoPlayer integration
- [x] Video texture management
- [x] Image loading & caching

### Phase 3: Rendering Pipeline ✅
- [x] MultiLayerCompositor
- [x] Filter system
- [x] Transform system
- [x] Overlay blending

### Phase 4: Real-time Preview ✅
- [x] PreviewRenderer
- [x] Render loop
- [x] TextureView integration

### Phase 5: Effects System ✅
- [x] Filter library
- [x] Transition library
- [x] Keyframe animation
- [x] Timeline layers

### Phase 6: Export ✅
- [x] Video encoder
- [x] Audio encoder
- [x] MediaMuxer integration

---

## 📖 References

- [ARCHITECTURE_CAPCUT_STYLE.md](./ARCHITECTURE_CAPCUT_STYLE.md) - Full architecture details
- [REALTIME_PREVIEW_EXPLAINED.md](./REALTIME_PREVIEW_EXPLAINED.md) - Preview mechanism
- [TIMELINE_LAYERS_EXPLAINED.md](./TIMELINE_LAYERS_EXPLAINED.md) - Timeline layers

---

## 🚀 Next Steps

1. **Implement Phase 1-2**: Core timeline + media playback
2. **Test với 1 video + 1 image**: Verify basic flow
3. **Add effects**: Filter, overlay, text
4. **Optimize performance**: Shader caching, texture pooling
5. **Polish UI**: Timeline view, effect picker

