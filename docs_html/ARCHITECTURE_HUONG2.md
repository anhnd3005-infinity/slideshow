# Kiến Trúc Hướng 2: ExoPlayer + GL Compositing

## Tổng Quan

Hệ thống mix ảnh + video với hiệu ứng realtime preview và export, dựa trên:
- **ExoPlayer**: decode video, play audio
- **OpenGL ES**: compositing, filter, transition, overlay
- **MediaCodec**: export video cuối cùng

---

## 1. Core Data Structures

### 1.1. MediaScene (thay thế Scene hiện tại)

```kotlin
enum class MediaType {
    IMAGE,
    VIDEO
}

data class MediaScene(
    val id: String,
    val type: MediaType,
    val uri: Uri,
    
    // Texture source (khác nhau tùy type)
    var imageTexture: Texture2d? = null,      // cho IMAGE
    var videoTexture: Texture2d? = null,      // cho VIDEO (OES texture)
    var exoPlayer: ExoPlayer? = null,         // cho VIDEO
    
    // Metadata
    var duration: Long = 3000L,
    var originalPath: String = "",
    
    // Effects (giống VideoComposer hiện tại)
    var transition: Transition = FadeTransition("fade", 1000L),
    var filter: PackFilter = PackFilter(),
    var cropType: BitmapProcessor.CropType = BitmapProcessor.CropType.FIT_CENTER,
    
    // Overlay layers
    val overlayLayers: MutableList<OverlayLayer> = mutableListOf()
)

data class OverlayLayer(
    val id: String,
    val texture: Texture2d,
    val zIndex: Int,
    val alpha: Float = 1f,
    val transform: Matrix4f = mat4()
)
```

### 1.2. Timeline Controller

```kotlin
class TimelineController {
    private val scenes = mutableListOf<MediaScene>()
    private var currentTime: Long = 0L
    private var totalDuration: Long = 0L
    
    fun addScene(scene: MediaScene) {
        scenes.add(scene)
        evaluateDuration()
    }
    
    fun getCurrentSceneIndex(): Int {
        var accumulated = 0L
        scenes.forEachIndexed { index, scene ->
            accumulated += scene.duration
            if (currentTime < accumulated) return index
        }
        return scenes.size - 1
    }
    
    fun getCurrentScene(): MediaScene? {
        val index = getCurrentSceneIndex()
        return scenes.getOrNull(index)
    }
    
    fun getNextScene(): MediaScene? {
        val index = getCurrentSceneIndex() + 1
        return scenes.getOrNull(index)
    }
    
    fun getProgressInCurrentScene(): Float {
        val index = getCurrentSceneIndex()
        var accumulated = 0L
        repeat(index) { accumulated += scenes[it].duration }
        val timeInScene = currentTime - accumulated
        return (timeInScene.toFloat() / scenes[index].duration.toFloat()).coerceIn(0f, 1f)
    }
    
    private fun evaluateDuration() {
        totalDuration = scenes.sumOf { it.duration }
    }
}
```

---

## 2. ExoPlayer Integration

### 2.1. VideoTextureManager

```kotlin
class VideoTextureManager(
    private val context: Context,
    private val glContext: EGLContext
) {
    private val players = mutableMapOf<String, ExoPlayer>()
    private val surfaceTextures = mutableMapOf<String, SurfaceTexture>()
    private val textures = mutableMapOf<String, Texture2d>()
    
    fun setupVideoScene(scene: MediaScene): Texture2d {
        val player = ExoPlayer.Builder(context).build()
        val surfaceTexture = SurfaceTexture(0) // OES texture ID
        val texture = Texture2d()
        
        // Setup OES texture
        texture.initialize()
        surfaceTexture.attachToGLContext(texture.id)
        
        // Setup ExoPlayer
        val surface = Surface(surfaceTexture)
        player.setVideoSurface(surface)
        player.setMediaItem(MediaItem.fromUri(scene.uri))
        player.prepare()
        player.playWhenReady = false // manual control
        
        players[scene.id] = player
        surfaceTextures[scene.id] = surfaceTexture
        textures[scene.id] = texture
        
        scene.exoPlayer = player
        scene.videoTexture = texture
        
        return texture
    }
    
    fun updateVideoTexture(sceneId: String, targetTimeMs: Long) {
        val player = players[sceneId] ?: return
        val surfaceTexture = surfaceTextures[sceneId] ?: return
        
        // Seek to target time
        player.seekTo(targetTimeMs)
        
        // Update texture
        surfaceTexture.updateTexImage()
    }
    
    fun release(sceneId: String) {
        players[sceneId]?.release()
        surfaceTextures[sceneId]?.release()
        textures[sceneId]?.release()
        players.remove(sceneId)
        surfaceTextures.remove(sceneId)
        textures.remove(sceneId)
    }
}
```

---

## 3. GL Rendering Pipeline

### 3.1. Compositor (thay thế VideoComposer)

```kotlin
class MediaCompositor(
    private val context: Context,
    private val glContext: EGLContext
) : StudioDrawable {
    
    private val timelineController = TimelineController()
    private val videoTextureManager = VideoTextureManager(context, glContext)
    
    // Shaders (giống VideoComposer)
    private val filterShader = PackFilterShader()
    private val transitionShader = TransitionalTextureShader()
    private val overlayShader = TextureShader()
    
    // FrameBuffers
    private val filterFBO = FrameBuffer()
    private val transitionFBO = FrameBuffer()
    private val finalFBO = FrameBuffer()
    
    fun addScene(scene: MediaScene) {
        when (scene.type) {
            MediaType.IMAGE -> setupImageScene(scene)
            MediaType.VIDEO -> setupVideoScene(scene)
        }
        timelineController.addScene(scene)
    }
    
    private fun setupImageScene(scene: MediaScene) {
        // Load bitmap → texture (giống VideoComposer hiện tại)
        val bitmap = BitmapProcessor.loadSync(scene.originalPath)
        val texture = Texture2d()
        texture.initialize()
        // ... setup texture từ bitmap
        scene.imageTexture = texture
    }
    
    private fun setupVideoScene(scene: MediaScene) {
        // Setup ExoPlayer → SurfaceTexture → OES texture
        videoTextureManager.setupVideoScene(scene)
    }
    
    override fun renderAtProgress(progress: Float) {
        val currentTime = (timelineController.totalDuration * progress).toLong()
        timelineController.currentTime = currentTime
        
        val currentScene = timelineController.getCurrentScene() ?: return
        val nextScene = timelineController.getNextScene()
        val progressInScene = timelineController.getProgressInCurrentScene()
        
        // Update video texture nếu cần
        if (currentScene.type == MediaType.VIDEO) {
            val targetTime = (currentScene.duration * progressInScene).toLong()
            videoTextureManager.updateVideoTexture(currentScene.id, targetTime)
        }
        
        // Get textures
        val currentTexture = when (currentScene.type) {
            MediaType.IMAGE -> currentScene.imageTexture!!
            MediaType.VIDEO -> currentScene.videoTexture!!
        }
        
        val nextTexture = nextScene?.let {
            when (it.type) {
                MediaType.IMAGE -> it.imageTexture
                MediaType.VIDEO -> it.videoTexture
            }
        }
        
        // Apply filter
        filterShader.use {
            filterShader.setTexture(currentTexture)
            filterShader.setFilter(currentScene.filter)
            // ... render to filterFBO
        }
        
        // Apply transition (nếu có next scene)
        if (nextTexture != null && progressInScene > 0.8f) {
            val transitionProgress = (progressInScene - 0.8f) / 0.2f
            transitionShader.use {
                transitionShader.setFromTexture(filterFBO.texture)
                transitionShader.setToTexture(nextTexture)
                transitionShader.setTransition(currentScene.transition)
                transitionShader.setProgress(transitionProgress)
                // ... render to transitionFBO
            }
        }
        
        // Apply overlay layers
        overlayShader.use {
            // Render từng overlay layer lên finalFBO
            currentScene.overlayLayers.sortedBy { it.zIndex }.forEach { layer ->
                overlayShader.setTexture(layer.texture)
                overlayShader.setAlpha(layer.alpha)
                overlayShader.setTransform(layer.transform)
                // ... render
            }
        }
        
        // Output to screen
        // finalFBO → TextureView
    }
}
```

---

## 4. Integration với SlideShowViewModel

### 4.1. Refactor SlideShowViewModel

```kotlin
class SlideShowViewModel : BaseViewModel() {
    
    // Thay VideoComposer bằng MediaCompositor
    private lateinit var mediaCompositor: MediaCompositor
    
    fun initDataBase(context: SlideShowActivity) {
        this.context = context
        
        // Init MediaCompositor thay vì VideoComposer
        mediaCompositor = MediaCompositor(context, glContext)
        
        // ... rest of init
    }
    
    fun loadDataImage(listImage: ArrayList<ImageModel>?) {
        if (listImage == null || listImage.isEmpty()) return
        
        // Convert ImageModel → MediaScene
        listImage.forEach { model ->
            val scene = if (model.isVideo) {
                createVideoScene(model.uriImage)
            } else {
                createImageScene(model.uriImage)
            }
            mediaCompositor.addScene(scene)
        }
    }
    
    private fun createImageScene(uri: Uri): MediaScene {
        // Load bitmap, process như cũ
        val bitmap = loadBitmapFromUri(uri)
        val processedBitmap = processBitmap(bitmap) // crop, compress, etc.
        val path = saveBitmapToCache(processedBitmap)
        
        return MediaScene(
            id = UUID.randomUUID().toString(),
            type = MediaType.IMAGE,
            uri = uri,
            originalPath = path,
            duration = 3000L // default
        )
    }
    
    private fun createVideoScene(uri: Uri): MediaScene {
        return MediaScene(
            id = UUID.randomUUID().toString(),
            type = MediaType.VIDEO,
            uri = uri,
            duration = getVideoDuration(uri) // từ MediaMetadataRetriever
        )
    }
}
```

---

## 5. Export Pipeline

### 5.1. Frame Recorder

```kotlin
class FrameRecorder(
    private val width: Int,
    private val height: Int,
    private val outputPath: String
) {
    private val mediaCodec = MediaCodec.createEncoderByType("video/avc")
    private val mediaMuxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MP4_2)
    
    fun recordFrame(texture: Texture2d, timestampUs: Long) {
        // Read texture → ByteBuffer
        val buffer = readTextureToBuffer(texture)
        
        // Encode frame
        val inputBufferIndex = mediaCodec.dequeueInputBuffer(0)
        if (inputBufferIndex >= 0) {
            val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
            inputBuffer?.put(buffer)
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, buffer.size, timestampUs, 0)
        }
        
        // Drain encoder
        drainEncoder()
    }
    
    fun finish() {
        mediaCodec.signalEndOfInputStream()
        drainEncoder()
        mediaCodec.release()
        mediaMuxer.release()
    }
}
```

---

## 6. Flow Diagram

```
User chọn ảnh + video
    ↓
MainViewModel.startImagePicker()
    ↓
Tạo ArrayList<ImageModel> (có isVideo flag)
    ↓
Start SlideShowActivity với intent
    ↓
SlideShowViewModel.loadDataImage()
    ↓
Với mỗi ImageModel:
    ├─ isVideo = false → createImageScene() → MediaCompositor.addScene()
    └─ isVideo = true  → createVideoScene() → MediaCompositor.addScene()
    ↓
MediaCompositor.setupVideoScene() cho mỗi video:
    ├─ Tạo ExoPlayer
    ├─ Tạo SurfaceTexture
    ├─ Tạo OES Texture2d
    └─ Link ExoPlayer → Surface → SurfaceTexture → Texture
    ↓
TimelineController quản lý:
    ├─ Current scene index
    ├─ Progress in scene
    └─ Total duration
    ↓
Render loop (mỗi frame):
    ├─ TimelineController.getCurrentScene()
    ├─ Nếu video: VideoTextureManager.updateVideoTexture() → seek ExoPlayer
    ├─ Get texture (image hoặc video)
    ├─ Apply filter shader
    ├─ Apply transition shader (nếu có next scene)
    ├─ Apply overlay layers
    └─ Render to TextureView
    ↓
Export (khi user click Save):
    ├─ FrameRecorder setup
    ├─ Loop qua từng frame theo timeline
    ├─ Render frame → record
    └─ Mux audio (từ ExoPlayer hoặc SoundManager)
```

---

## 7. Ưu & Nhược Điểm

### ✅ Ưu điểm:
- **Hỗ trợ video thật** (không phải thumbnail)
- **Giữ được toàn bộ effect** của VideoComposer (filter, transition, overlay)
- **Preview realtime** mượt
- **Export chất lượng cao** (MediaCodec)
- **Kiến trúc rõ ràng**, dễ maintain

### ⚠️ Nhược điểm:
- **Phức tạp hơn** VideoComposer hiện tại
- **Cần quản lý nhiều ExoPlayer** (mỗi video = 1 player)
- **Memory overhead** (nhiều texture, FBO)
- **Cần refactor lớn** từ code hiện tại

---

## 8. Migration Path

### Phase 1: Setup infrastructure
- Tạo `MediaScene`, `TimelineController`
- Tạo `VideoTextureManager`
- Test với 1 video đơn giản

### Phase 2: Integrate với image
- Port logic image từ VideoComposer
- Test mix 1 image + 1 video

### Phase 3: Port effects
- Port filter shader
- Port transition shader
- Port overlay system

### Phase 4: Export
- Implement FrameRecorder
- Test export với mix image + video

### Phase 5: Polish
- Optimize memory
- Handle edge cases
- UI/UX improvements

