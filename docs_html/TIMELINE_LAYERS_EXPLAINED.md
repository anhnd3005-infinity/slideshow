# Timeline Layers - Chèn Hiệu Ứng Theo Thời Gian

## ✅ Hiểu Rồi! Mỗi Layer Có Timeline Riêng

Bạn muốn:
- **Clip gốc**: Video/ảnh chạy từ 0s → 30s
- **Layer nhạc**: Chỉ chạy từ **0s → 10s**
- **Layer phủ**: Chỉ chạy từ **5s → 20s**
- **Layer sticker**: Chỉ chạy từ **15s → 25s**
- **Layer text**: Chỉ chạy từ **8s → 12s**

→ Mỗi layer có **startTime** và **duration** riêng, độc lập với clip gốc!

---

## 🎯 Cách Hoạt Động

### 1. Timeline Structure

```kotlin
// Clip gốc (video/ảnh)
val mainClip = Clip(
    type = ClipType.VIDEO,
    source = MediaSource(videoUri),
    startTime = 0L,        // Bắt đầu từ 0s
    duration = 30000L      // Kéo dài 30s
)

// Layer nhạc (0s - 10s)
val musicClip = Clip(
    type = ClipType.AUDIO,
    source = MediaSource(musicUri),
    startTime = 0L,        // Bắt đầu từ 0s
    duration = 10000L      // Kéo dài 10s
)

// Layer phủ (5s - 20s)
val overlayClip = Clip(
    type = ClipType.EFFECT_OVERLAY,
    source = MediaSource(framePngUri),
    startTime = 5000L,     // Bắt đầu từ 5s
    duration = 15000L      // Kéo dài 15s (đến 20s)
)

// Layer sticker (15s - 25s)
val stickerClip = Clip(
    type = ClipType.STICKER,
    source = MediaSource(stickerUri),
    startTime = 15000L,    // Bắt đầu từ 15s
    duration = 10000L      // Kéo dài 10s (đến 25s)
)

// Layer text (8s - 12s)
val textClip = Clip(
    type = ClipType.TEXT,
    source = TextSource("Hello World"),
    startTime = 8000L,     // Bắt đầu từ 8s
    duration = 4000L       // Kéo dài 4s (đến 12s)
)
```

### 2. Add vào Timeline

```kotlin
val timelineController = TimelineController()

// Tạo các tracks
val videoTrack = timelineController.addTrack(TrackType.VIDEO)
val audioTrack = timelineController.addTrack(TrackType.AUDIO)
val overlayTrack = timelineController.addTrack(TrackType.OVERLAY)
val textTrack = timelineController.addTrack(TrackType.TEXT)

// Add clips vào tracks tương ứng
videoTrack.addClip(mainClip)
audioTrack.addClip(musicClip)
overlayTrack.addClip(overlayClip)
textTrack.addClip(stickerClip)
textTrack.addClip(textClip)
```

### 3. Query Clips Tại Thời Điểm Cụ Thể

```kotlin
// Tại thời điểm 0s
val clipsAt0s = timelineController.getClipsAtTime(0L)
// → videoClip (0-30s) ✅
// → musicClip (0-10s) ✅
// → overlayClip: KHÔNG (chưa bắt đầu)
// → stickerClip: KHÔNG (chưa bắt đầu)
// → textClip: KHÔNG (chưa bắt đầu)

// Tại thời điểm 7s
val clipsAt7s = timelineController.getClipsAtTime(7000L)
// → videoClip (0-30s) ✅
// → musicClip (0-10s) ✅
// → overlayClip (5-20s) ✅
// → textClip (8-12s): KHÔNG (chưa bắt đầu)

// Tại thời điểm 10s
val clipsAt10s = timelineController.getClipsAtTime(10000L)
// → videoClip (0-30s) ✅
// → musicClip: KHÔNG (đã kết thúc ở 10s)
// → overlayClip (5-20s) ✅
// → textClip (8-12s) ✅

// Tại thời điểm 18s
val clipsAt18s = timelineController.getClipsAtTime(18000L)
// → videoClip (0-30s) ✅
// → overlayClip (5-20s) ✅
// → stickerClip (15-25s) ✅
// → textClip: KHÔNG (đã kết thúc ở 12s)

// Tại thời điểm 25s
val clipsAt25s = timelineController.getClipsAtTime(25000L)
// → videoClip (0-30s) ✅
// → overlayClip: KHÔNG (đã kết thúc ở 20s)
// → stickerClip: KHÔNG (đã kết thúc ở 25s)
```

---

## 🎬 Render Pipeline

### Render Frame Tại Thời Điểm Cụ Thể

```kotlin
class MultiLayerCompositor {
    fun renderFrame(time: Long) {
        // Query tất cả clips đang active tại thời điểm này
        val clipsAtTime = timelineController.getClipsAtTime(time)
        
        // Render video/image track (base layer)
        val videoClips = clipsAtTime[TrackType.VIDEO] ?: emptyList()
        val baseTexture = renderVideoTrack(videoClips, time)
        
        // Render overlay track (nếu có)
        val overlayClips = clipsAtTime[TrackType.OVERLAY] ?: emptyList()
        overlayClips.forEach { overlayClip ->
            val overlayTexture = getTextureForClip(overlayClip, time)
            blendShader.apply(overlayTexture, baseTexture)
        }
        
        // Render text track (nếu có)
        val textClips = clipsAtTime[TrackType.TEXT] ?: emptyList()
        textClips.forEach { textClip ->
            renderText(textClip, time)
        }
        
        // Mix audio (nếu có)
        val audioClips = clipsAtTime[TrackType.AUDIO] ?: emptyList()
        audioClips.forEach { audioClip ->
            audioMixer.mixAudio(audioClip, time)
        }
    }
}
```

---

## 📝 Ví Dụ Cụ Thể: Timeline Phức Tạp

### Scenario: Video 30s với nhiều layers

```kotlin
fun setupComplexTimeline() {
    val timeline = TimelineController()
    
    // === VIDEO TRACK ===
    val videoTrack = timeline.addTrack(TrackType.VIDEO)
    val mainVideo = Clip(
        type = ClipType.VIDEO,
        source = MediaSource(videoUri),
        startTime = 0L,
        duration = 30000L  // 0s - 30s
    )
    videoTrack.addClip(mainVideo)
    
    // === AUDIO TRACK ===
    val audioTrack = timeline.addTrack(TrackType.AUDIO)
    
    // Nhạc nền 1: 0s - 10s
    val bgMusic1 = Clip(
        type = ClipType.AUDIO,
        source = MediaSource(music1Uri),
        startTime = 0L,
        duration = 10000L,
        volume = 0.8f
    )
    audioTrack.addClip(bgMusic1)
    
    // Nhạc nền 2: 15s - 30s
    val bgMusic2 = Clip(
        type = ClipType.AUDIO,
        source = MediaSource(music2Uri),
        startTime = 15000L,
        duration = 15000L,
        volume = 0.7f
    )
    audioTrack.addClip(bgMusic2)
    
    // Sound effect: 5s - 7s
    val sfx = Clip(
        type = ClipType.AUDIO,
        source = MediaSource(sfxUri),
        startTime = 5000L,
        duration = 2000L,
        volume = 1.0f
    )
    audioTrack.addClip(sfx)
    
    // === OVERLAY TRACK ===
    val overlayTrack = timeline.addTrack(TrackType.OVERLAY)
    
    // Khung ảnh: 5s - 20s
    val frameOverlay = Clip(
        type = ClipType.EFFECT_OVERLAY,
        source = MediaSource(framePngUri),
        startTime = 5000L,
        duration = 15000L,
        transform = Transform(opacity = 0.9f)
    )
    overlayTrack.addClip(frameOverlay)
    
    // Vignette effect: 10s - 25s
    val vignette = Clip(
        type = ClipType.EFFECT_OVERLAY,
        source = MediaSource(vignetteShader),
        startTime = 10000L,
        duration = 15000L,
        transform = Transform(opacity = 0.5f)
    )
    overlayTrack.addClip(vignette)
    
    // === TEXT TRACK ===
    val textTrack = timeline.addTrack(TrackType.TEXT)
    
    // Title: 0s - 5s
    val title = Clip(
        type = ClipType.TEXT,
        source = TextSource("Welcome!"),
        startTime = 0L,
        duration = 5000L,
        transform = Transform(
            position = PointF(0.5f, 0.2f),
            scale = 1.5f
        )
    )
    textTrack.addClip(title)
    
    // Subtitle: 8s - 12s
    val subtitle = Clip(
        type = ClipType.TEXT,
        source = TextSource("This is a subtitle"),
        startTime = 8000L,
        duration = 4000L,
        transform = Transform(
            position = PointF(0.5f, 0.8f),
            scale = 1.0f
        )
    )
    textTrack.addClip(subtitle)
    
    // === STICKER TRACK ===
    val stickerTrack = timeline.addTrack(TrackType.OVERLAY)
    
    // Sticker 1: 15s - 25s
    val sticker1 = Clip(
        type = ClipType.STICKER,
        source = MediaSource(sticker1Uri),
        startTime = 15000L,
        duration = 10000L,
        transform = Transform(
            position = PointF(0.2f, 0.3f),
            scale = 0.5f
        )
    )
    stickerTrack.addClip(sticker1)
    
    // Sticker 2: 20s - 28s
    val sticker2 = Clip(
        type = ClipType.STICKER,
        source = MediaSource(sticker2Uri),
        startTime = 20000L,
        duration = 8000L,
        transform = Transform(
            position = PointF(0.8f, 0.7f),
            scale = 0.4f
        )
    )
    stickerTrack.addClip(sticker2)
}
```

### Timeline Visualization

```
Time:    0s    5s    10s   15s   20s   25s   30s
        |-----|-----|-----|-----|-----|-----|
VIDEO:  [====================================]
        |-----|-----|-----|-----|-----|-----|
AUDIO:  [========]     [====================]
        [==] (sfx)
        |-----|-----|-----|-----|-----|-----|
OVERLAY:     [===============]
             [=======================] (vignette)
        |-----|-----|-----|-----|-----|-----|
TEXT:   [====]
        [====] (subtitle)
        |-----|-----|-----|-----|-----|-----|
STICKER:            [==========]
                    [========] (sticker2)
```

---

## 🎨 UI Timeline View

### Visual Representation

```kotlin
class TimelineView : View {
    override fun onDraw(canvas: Canvas) {
        val timeline = timelineController
        
        // Draw time ruler
        drawTimeRuler(canvas)
        
        // Draw each track
        var y = 0f
        TrackType.values().forEach { trackType ->
            val track = timeline.getTrack(trackType) ?: return@forEach
            
            // Draw track background
            canvas.drawRect(0f, y, width.toFloat(), y + trackHeight, trackPaint)
            
            // Draw clips in this track
            track.clips.forEach { clip ->
                val x = clip.startTime / 1000f * pixelsPerSecond
                val width = clip.duration / 1000f * pixelsPerSecond
                
                // Draw clip rectangle
                canvas.drawRect(x, y, x + width, y + trackHeight, clipPaint)
                
                // Draw clip name/icon
                drawClipLabel(canvas, clip, x, y)
            }
            
            y += trackHeight
        }
        
        // Draw playhead
        val playheadX = timeline.currentTime / 1000f * pixelsPerSecond
        canvas.drawLine(playheadX, 0f, playheadX, height.toFloat(), playheadPaint)
    }
}
```

---

## ⚡ Performance: Query Optimization

### Efficient Clip Lookup

```kotlin
class Track {
    private val clipsSortedByStart = mutableListOf<Clip>()
    private val clipsSortedByEnd = mutableListOf<Clip>()
    
    fun addClip(clip: Clip) {
        clipsSortedByStart.add(clip)
        clipsSortedByStart.sortBy { it.startTime }
        
        clipsSortedByEnd.add(clip)
        clipsSortedByEnd.sortBy { it.endTime }
    }
    
    fun getClipsAtTime(time: Long): List<Clip> {
        // Binary search optimization
        val startIndex = clipsSortedByStart.binarySearch { 
            it.startTime.compareTo(time) 
        }
        
        val endIndex = clipsSortedByEnd.binarySearch { 
            it.endTime.compareTo(time) 
        }
        
        // Only check clips that could be active
        return clipsSortedByStart
            .subList(0, startIndex)
            .filter { time < it.endTime }
    }
}
```

---

## ✅ Kết Luận

**Hoàn toàn hỗ trợ** việc chèn layers/hiệu ứng vào các khoảng thời gian cụ thể:

1. ✅ Mỗi clip có `startTime` và `duration` riêng
2. ✅ `getClipsAtTime()` query chính xác clips đang active
3. ✅ Render pipeline chỉ render clips đang active tại thời điểm đó
4. ✅ UI timeline hiển thị rõ ràng vị trí của từng layer
5. ✅ Performance được optimize bằng binary search

**Ví dụ timeline của bạn:**
- 0-10s: Nhạc ✅
- 5-20s: Lớp phủ ✅
- 15-25s: Sticker ✅
- 8-12s: Text ✅

→ Tất cả đều được hỗ trợ!

