# Real-Time Preview Khi Apply Effect

## ✅ Câu Trả Lời Ngắn: **CÓ, Preview Ngay Lập Tức**

Khi bạn apply effect (filter, transition, overlay, v.v.), preview sẽ **ngay lập tức** vì:

1. **Render loop chạy liên tục** (30-60fps)
2. **Effect được apply trong render loop**, không cần "build" hay "compile"
3. **Chỉ cần update property** của clip → frame tiếp theo sẽ render với effect mới

---

## 🔄 Cách Hoạt Động

### 1. Render Loop (Chạy Liên Tục)

```kotlin
class PreviewRenderer {
    private val renderThread = HandlerThread("RenderThread")
    private val handler = Handler(renderThread.looper)
    
    private var isPlaying = false
    private var currentTime = 0L
    
    fun startPreview() {
        isPlaying = true
        renderLoop()
    }
    
    private fun renderLoop() {
        if (!isPlaying) return
        
        // Render frame tại thời điểm hiện tại
        renderFrame(currentTime)
        
        // Update time (30fps = 33ms per frame)
        currentTime += 33
        
        // Schedule next frame
        handler.postDelayed({ renderLoop() }, 33)
    }
    
    private fun renderFrame(time: Long) {
        // Lấy clips tại thời điểm này
        val clips = timelineController.getClipsAtTime(time)
        
        // Render với effects hiện tại
        compositor.renderFrame(time, clips)
        
        // Update TextureView
        textureView.setTexture(compositor.getOutputTexture())
    }
}
```

### 2. Khi User Apply Effect

```kotlin
// User click "Apply Filter: Vivid"
fun applyFilterToClip(clipId: String, filter: Filter) {
    // 1. Tìm clip trong timeline
    val clip = timelineController.findClip(clipId)
    
    // 2. Update filter property (CHỈ UPDATE DATA, KHÔNG RENDER NGAY)
    clip.filter = filter
    
    // 3. Render loop sẽ tự động pick up change ở frame tiếp theo!
    // Không cần gọi render() thủ công
}
```

### 3. Render Pipeline (Mỗi Frame)

```kotlin
class MultiLayerCompositor {
    fun renderFrame(time: Long, clips: Map<TrackType, List<Clip>>) {
        // Lấy video/image clip
        val videoClip = clips[TrackType.VIDEO]?.firstOrNull()
        
        // Get texture (từ ExoPlayer hoặc Bitmap)
        val baseTexture = getTextureForClip(videoClip, time)
        
        // ✅ Apply filter (đọc từ clip.filter - đã được update ở trên)
        val filteredTexture = if (videoClip?.filter != null) {
            filterShader.apply(baseTexture, videoClip.filter!!)
        } else {
            baseTexture
        }
        
        // ✅ Apply transform (có thể có keyframes)
        val transformedTexture = transformShader.apply(
            filteredTexture, 
            calculateTransform(videoClip, time)
        )
        
        // ✅ Apply overlay layers
        val overlayClips = clips[TrackType.OVERLAY] ?: emptyList()
        overlayClips.forEach { overlayClip ->
            blendShader.apply(overlayClip.texture, transformedTexture)
        }
        
        // ✅ Render text layers
        val textClips = clips[TrackType.TEXT] ?: emptyList()
        textClips.forEach { textClip ->
            textShader.render(textClip, transformedTexture)
        }
        
        // Output to screen
        return finalTexture
    }
}
```

---

## ⚡ Flow Khi Apply Effect

```
User Action:
  Click "Apply Filter: Vivid"
    ↓
ViewModel:
  clip.filter = FilterLibrary.VIVID
    ↓
TimelineController:
  clip được update (chỉ data, không render)
    ↓
Render Loop (đang chạy):
  Frame N: Render với filter cũ
    ↓
  Frame N+1 (33ms sau):
    renderFrame() được gọi
    → Đọc clip.filter (đã là VIVID)
    → Apply filter shader
    → Render với effect mới
    ↓
Screen:
  ✅ Preview ngay lập tức!
```

**Thời gian delay**: Chỉ **1 frame** (33ms @ 30fps hoặc 16ms @ 60fps) → **Cảm giác như instant**

---

## 🎯 Ví Dụ Cụ Thể

### Case 1: Apply Filter

```kotlin
// User đang preview video, click button "Vivid"
buttonVivid.setOnClickListener {
    val currentClip = getSelectedClip()
    
    // Update filter
    currentClip.filter = FilterLibrary.getFilter("vivid")
    
    // ✅ Preview ngay! Render loop sẽ tự động apply ở frame tiếp theo
    // Không cần gọi gì thêm
}
```

### Case 2: Add Overlay Sticker

```kotlin
// User drag sticker vào timeline
fun addSticker(stickerUri: Uri, time: Long) {
    val stickerClip = Clip(
        type = ClipType.STICKER,
        source = MediaSource(stickerUri),
        startTime = time,
        duration = 5000L
    )
    
    // Load texture
    stickerClip.texture = loadStickerTexture(stickerUri)
    
    // Add to overlay track
    timelineController.getTrack(TrackType.OVERLAY)?.addClip(stickerClip)
    
    // ✅ Preview ngay! Sticker sẽ xuất hiện ở frame tiếp theo
}
```

### Case 3: Change Transform (Position, Scale)

```kotlin
// User drag clip để thay đổi position
fun updateClipPosition(clipId: String, newPosition: PointF) {
    val clip = timelineController.findClip(clipId)
    
    // Update transform
    clip.transform.position = newPosition
    
    // ✅ Preview ngay! Clip sẽ di chuyển ngay lập tức
}
```

### Case 4: Add Keyframe Animation

```kotlin
// User set keyframe tại thời điểm hiện tại
fun addKeyframe(clipId: String) {
    val clip = timelineController.findClip(clipId)
    val currentTime = previewRenderer.currentTime
    
    val keyframe = Keyframe(
        time = currentTime - clip.startTime,
        transform = clip.transform.copy()
    )
    
    clip.keyframes.add(keyframe)
    
    // ✅ Preview ngay! Animation sẽ được interpolate ngay lập tức
}
```

---

## 🚀 Performance Optimization

### 1. Lazy Texture Loading

```kotlin
// Chỉ load texture khi cần render
fun getTextureForClip(clip: Clip, time: Long): Texture2d {
    // Check if clip is visible at this time
    if (time < clip.startTime || time >= clip.endTime) {
        return emptyTexture  // Skip loading
    }
    
    // Load on-demand
    return when (clip.type) {
        ClipType.VIDEO -> {
            if (!videoTextures.containsKey(clip.id)) {
                setupVideoTexture(clip)
            }
            updateVideoTexture(clip, time)
            videoTextures[clip.id]!!
        }
        ClipType.IMAGE -> {
            imageCache.getOrLoad(clip.source.uri)
        }
        // ...
    }
}
```

### 2. Shader Caching

```kotlin
// Cache compiled shaders
private val shaderCache = mutableMapOf<String, Int>()

fun getShader(shaderCode: String): Int {
    return shaderCache.getOrPut(shaderCode) {
        compileShader(shaderCode)  // Expensive operation
    }
}
```

### 3. Frame Skipping (Khi Preview Quality Low)

```kotlin
fun renderFrame(time: Long) {
    // Skip frames nếu đang scrub nhanh
    if (isScrubbing && shouldSkipFrame()) {
        return
    }
    
    // Render với quality thấp hơn khi preview
    val quality = if (isExporting) 1.0f else 0.7f  // Scale down
    compositor.renderFrame(time, quality)
}
```

---

## 📊 So Sánh với CapCut

| Feature | CapCut | Our Architecture |
|---------|--------|------------------|
| **Preview khi apply filter** | ✅ Instant | ✅ Instant (1 frame delay) |
| **Preview khi add overlay** | ✅ Instant | ✅ Instant |
| **Preview khi change transform** | ✅ Instant | ✅ Instant |
| **Preview khi scrub timeline** | ✅ Smooth | ✅ Smooth (có thể skip frame) |
| **Preview quality** | High | Configurable (High/Low) |
| **Export quality** | High | High (full resolution) |

---

## ⚠️ Edge Cases

### 1. Heavy Effect (Blur nhiều pass)

```kotlin
// Nếu effect quá nặng, có thể drop frame
fun renderFrame(time: Long) {
    val startTime = System.currentTimeMillis()
    
    compositor.renderFrame(time)
    
    val renderTime = System.currentTimeMillis() - startTime
    
    // Nếu render > 33ms, log warning
    if (renderTime > 33) {
        Log.w("Preview", "Frame dropped: ${renderTime}ms")
    }
}
```

### 2. Multiple Heavy Effects

```kotlin
// Giảm quality khi có nhiều effect
fun shouldReduceQuality(): Boolean {
    val heavyEffectCount = clips.count { 
        it.filter?.isHeavy == true || 
        it.overlayLayers.size > 5 
    }
    
    return heavyEffectCount > 3
}
```

---

## ✅ Kết Luận

**Preview ngay lập tức khi apply effect** là **hoàn toàn khả thi** vì:

1. ✅ Render loop chạy liên tục
2. ✅ Effect chỉ là **data property** → update ngay
3. ✅ Render pipeline đọc property mới ở frame tiếp theo
4. ✅ Delay chỉ 1 frame (33ms) → cảm giác instant

**Điều kiện để đảm bảo smooth**:
- Render mỗi frame < 33ms (30fps) hoặc < 16ms (60fps)
- Optimize shader, texture loading
- Có thể giảm quality khi preview nếu cần

