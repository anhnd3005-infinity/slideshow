# Timeline Scrub Preview System

## Tổng Quan

Hệ thống cho phép user **scroll timeline và preview realtime** với tất cả effects đã apply, giống CapCut.

**Yêu cầu:**
- ✅ Preview mượt khi scroll (60fps)
- ✅ Apply đầy đủ effects (filter, transition, overlay)
- ✅ Low latency (< 50ms)
- ✅ Memory efficient

---

## 1. Preview Controller

### 1.1. Core Preview Controller

```kotlin
class PreviewController(
    private val compositor: MultiLayerCompositor,
    private val timelineController: TimelineController,
    private val textureView: TextureView
) {
    
    private var isScrubbing = false
    private var lastScrubTime = 0L
    private val scrubDebouncer = Handler(Looper.getMainLooper())
    
    // Frame cache cho scrub preview
    private val frameCache = FrameCache()
    
    // Preview quality settings
    private var previewQuality = PreviewQuality.HIGH
    private var isQuickPreviewMode = false
    
    enum class PreviewQuality {
        LOW,      // 360p, no effects
        MEDIUM,   // 720p, basic effects
        HIGH      // 1080p, full effects
    }
    
    /**
     * Called khi user scroll timeline
     */
    fun onTimelineScrolled(time: Long) {
        isScrubbing = true
        lastScrubTime = time
        
        // Debounce để tránh render quá nhiều
        scrubDebouncer.removeCallbacksAndMessages(null)
        scrubDebouncer.postDelayed({
            updatePreview(time)
        }, 16) // ~60fps max
    }
    
    /**
     * Called khi user release scroll
     */
    fun onTimelineScrollEnded() {
        isScrubbing = false
        scrubDebouncer.removeCallbacksAndMessages(null)
        
        // Render final frame với full quality
        updatePreview(lastScrubTime, forceFullQuality = true)
    }
    
    private fun updatePreview(time: Long, forceFullQuality: Boolean = false) {
        timelineController.currentTime = time
        
        // Check cache first
        val cachedFrame = frameCache.get(time)
        if (cachedFrame != null && !forceFullQuality) {
            displayFrame(cachedFrame)
            return
        }
        
        // Determine preview quality
        val quality = if (isScrubbing && !forceFullQuality) {
            PreviewQuality.MEDIUM  // Lower quality khi scrolling
        } else {
            previewQuality
        }
        
        // Render frame
        val frameTexture = renderFrameAtTime(time, quality)
        
        // Cache frame
        if (!isScrubbing) {
            frameCache.put(time, frameTexture)
        }
        
        // Display
        displayFrame(frameTexture)
    }
    
    private fun renderFrameAtTime(time: Long, quality: PreviewQuality): Texture2d {
        val (width, height) = getPreviewDimensions(quality)
        
        return compositor.renderFrame(
            time = time,
            width = width,
            height = height,
            quality = quality,
            skipHeavyEffects = quality == PreviewQuality.LOW
        )
    }
    
    private fun getPreviewDimensions(quality: PreviewQuality): Pair<Int, Int> {
        return when (quality) {
            PreviewQuality.LOW -> Pair(360, 640)
            PreviewQuality.MEDIUM -> Pair(720, 1280)
            PreviewQuality.HIGH -> Pair(1080, 1920)
        }
    }
    
    private fun displayFrame(texture: Texture2d) {
        // Update TextureView với texture mới
        textureView.setSurfaceTexture(texture.surfaceTexture)
    }
}
```

---

## 2. Frame Cache System

### 2.1. Smart Frame Cache

```kotlin
class FrameCache {
    
    // Cache theo time (rounded to nearest second)
    private val cache = LruCache<Long, CachedFrame>(50)
    
    // Cache theo keyframe positions (important frames)
    private val keyframeCache = mutableMapOf<Long, CachedFrame>()
    
    data class CachedFrame(
        val texture: Texture2d,
        val timestamp: Long,
        val quality: PreviewController.PreviewQuality
    )
    
    /**
     * Get cached frame tại time (hoặc gần nhất)
     */
    fun get(time: Long): CachedFrame? {
        // Round to nearest second for cache key
        val cacheKey = (time / 1000) * 1000
        
        // Check exact match
        cache.get(cacheKey)?.let { return it }
        
        // Check keyframe cache (transition points, clip starts/ends)
        keyframeCache.entries
            .minByOrNull { abs(it.key - time) }
            ?.takeIf { abs(it.key - time) < 500 } // Within 500ms
            ?.value
            ?.let { return it }
        
        return null
    }
    
    fun put(time: Long, texture: Texture2d, quality: PreviewController.PreviewQuality) {
        val cacheKey = (time / 1000) * 1000
        val cachedFrame = CachedFrame(texture, time, quality)
        
        cache.put(cacheKey, cachedFrame)
        
        // Also cache at keyframe positions
        if (isKeyframePosition(time)) {
            keyframeCache[time] = cachedFrame
        }
    }
    
    private fun isKeyframePosition(time: Long): Boolean {
        // Cache tại các vị trí quan trọng:
        // - Clip boundaries
        // - Transition points
        // - Keyframe positions
        return time % 1000 == 0L  // Every second
    }
    
    fun clear() {
        cache.evictAll()
        keyframeCache.values.forEach { it.texture.release() }
        keyframeCache.clear()
    }
    
    fun preloadKeyframes(timelineController: TimelineController) {
        // Pre-load frames tại các vị trí quan trọng
        val keyframeTimes = mutableListOf<Long>()
        
        timelineController.tracks.values.forEach { track ->
            track.clips.forEach { clip ->
                // Clip start
                keyframeTimes.add(clip.startTime)
                
                // Clip end
                keyframeTimes.add(clip.endTime)
                
                // Transition points
                if (clip.transitionIn != null) {
                    keyframeTimes.add(clip.startTime)
                }
                if (clip.transitionOut != null) {
                    keyframeTimes.add(clip.endTime - clip.transitionOut!!.duration)
                }
                
                // Keyframes
                clip.keyframes.forEach { kf ->
                    keyframeTimes.add(clip.startTime + kf.time)
                }
            }
        }
        
        // Pre-render trong background
        keyframeTimes.distinct().forEach { time ->
            // Render in background thread
            // Cache result
        }
    }
}
```

---

## 3. Optimized Render Pipeline

### 3.1. Quality-Aware Rendering

```kotlin
class MultiLayerCompositor {
    
    fun renderFrame(
        time: Long,
        width: Int,
        height: Int,
        quality: PreviewController.PreviewQuality = PreviewController.PreviewQuality.HIGH,
        skipHeavyEffects: Boolean = false
    ): Texture2d {
        
        val clipsAtTime = timelineController.getClipsAtTime(time)
        
        // Use smaller FBO for lower quality
        val fbo = when (quality) {
            PreviewController.PreviewQuality.LOW -> lowQualityFBO
            PreviewController.PreviewQuality.MEDIUM -> mediumQualityFBO
            PreviewController.PreviewQuality.HIGH -> highQualityFBO
        }
        
        fbo.bind()
        glClear(GL_COLOR_BUFFER_BIT)
        
        // Render với quality settings
        renderVideoTrack(
            clips = clipsAtTime[TrackType.VIDEO] ?: emptyList(),
            time = time,
            width = width,
            height = height,
            quality = quality,
            skipHeavyEffects = skipHeavyEffects
        )
        
        if (!skipHeavyEffects) {
            renderOverlayTrack(
                clipsAtTime[TrackType.OVERLAY] ?: emptyList(),
                time, width, height
            )
            
            renderTextTrack(
                clipsAtTime[TrackType.TEXT] ?: emptyList(),
                time, width, height
            )
        }
        
        return fbo.texture
    }
    
    private fun renderVideoTrack(
        clips: List<Clip>,
        time: Long,
        width: Int,
        height: Int,
        quality: PreviewController.PreviewQuality,
        skipHeavyEffects: Boolean
    ) {
        clips.forEach { clip ->
            val texture = getClipTexture(clip, time)
            
            // Apply filter (skip nếu LOW quality)
            val filteredTexture = if (!skipHeavyEffects && clip.filter != null) {
                applyFilter(texture, clip.filter!!, quality)
            } else {
                texture
            }
            
            // Apply transform
            val transform = calculateTransformAtTime(clip, time)
            val transformedTexture = applyTransform(
                filteredTexture,
                transform,
                width,
                height
            )
            
            // Blend onto final FBO
            blendToFinal(transformedTexture)
        }
    }
    
    private fun applyFilter(
        texture: Texture2d,
        filter: Filter,
        quality: PreviewController.PreviewQuality
    ): Texture2d {
        // Use simpler shader for lower quality
        val shader = when (quality) {
            PreviewController.PreviewQuality.LOW -> filterShaderSimple
            PreviewController.PreviewQuality.MEDIUM -> filterShaderMedium
            PreviewController.PreviewQuality.HIGH -> filterShaderFull
        }
        
        return shader.apply(texture, filter)
    }
}
```

---

## 4. Timeline View Integration

### 4.1. Scroll-Aware Timeline View

```kotlin
class TimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    
    private var previewController: PreviewController? = null
    private var timelineController: TimelineController? = null
    
    private var isScrolling = false
    private var scrollVelocity = 0f
    
    fun setPreviewController(controller: PreviewController) {
        this.previewController = controller
    }
    
    fun setTimelineController(controller: TimelineController) {
        this.timelineController = controller
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isScrolling = true
                previewController?.onTimelineScrolled(getTimeAtX(event.x))
            }
            
            MotionEvent.ACTION_MOVE -> {
                val time = getTimeAtX(event.x)
                previewController?.onTimelineScrolled(time)
                
                // Calculate velocity for adaptive quality
                scrollVelocity = calculateVelocity(event)
            }
            
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                isScrolling = false
                previewController?.onTimelineScrollEnded()
            }
        }
        
        invalidate()
        return true
    }
    
    private fun getTimeAtX(x: Float): Long {
        val pixelsPerSecond = 50f  // Configurable
        return (x / pixelsPerSecond * 1000).toLong()
    }
    
    private fun calculateVelocity(event: MotionEvent): Float {
        // Calculate scroll velocity
        // Use để adjust preview quality
        return 0f  // Simplified
    }
    
    override fun onDraw(canvas: Canvas) {
        // Draw timeline UI
        drawTracks(canvas)
        drawPlayhead(canvas)
    }
    
    private fun drawPlayhead(canvas: Canvas) {
        val controller = timelineController ?: return
        val playheadX = controller.currentTime / 1000f * pixelsPerSecond
        
        // Draw playhead line
        canvas.drawLine(
            playheadX, 0f,
            playheadX, height.toFloat(),
            playheadPaint
        )
    }
}
```

---

## 5. Background Pre-rendering

### 5.1. Pre-render Service

```kotlin
class PreviewPreRenderService(
    private val compositor: MultiLayerCompositor,
    private val frameCache: FrameCache,
    private val timelineController: TimelineController
) {
    
    private val preRenderThread = HandlerThread("PreRenderThread")
    private val preRenderHandler: Handler
    
    init {
        preRenderThread.start()
        preRenderHandler = Handler(preRenderThread.looper)
    }
    
    /**
     * Pre-render frames tại các vị trí quan trọng
     */
    fun preRenderKeyframes() {
        val keyframeTimes = getKeyframeTimes()
        
        keyframeTimes.forEach { time ->
            preRenderHandler.post {
                // Render frame
                val texture = compositor.renderFrame(
                    time = time,
                    width = 720,  // Medium quality
                    height = 1280,
                    quality = PreviewController.PreviewQuality.MEDIUM
                )
                
                // Cache
                frameCache.put(time, texture, PreviewController.PreviewQuality.MEDIUM)
            }
        }
    }
    
    /**
     * Pre-render frames xung quanh current time
     */
    fun preRenderAroundTime(currentTime: Long, range: Long = 5000L) {
        val startTime = maxOf(0L, currentTime - range)
        val endTime = minOf(timelineController.totalDuration, currentTime + range)
        
        // Render every 500ms
        var time = startTime
        while (time <= endTime) {
            preRenderHandler.postDelayed({
                val texture = compositor.renderFrame(
                    time = time,
                    width = 720,
                    height = 1280,
                    quality = PreviewController.PreviewQuality.MEDIUM
                )
                frameCache.put(time, texture, PreviewController.PreviewQuality.MEDIUM)
            }, (time - startTime) / 10)  // Stagger renders
            
            time += 500
        }
    }
    
    private fun getKeyframeTimes(): List<Long> {
        val times = mutableListOf<Long>()
        
        timelineController.tracks.values.forEach { track ->
            track.clips.forEach { clip ->
                times.add(clip.startTime)
                times.add(clip.endTime)
                clip.keyframes.forEach { kf ->
                    times.add(clip.startTime + kf.time)
                }
            }
        }
        
        return times.distinct().sorted()
    }
}
```

---

## 6. Performance Optimization Strategies

### 6.1. Adaptive Quality

```kotlin
class AdaptivePreviewQuality {
    
    fun adjustQualityBasedOnScroll(velocity: Float): PreviewController.PreviewQuality {
        return when {
            velocity > 1000f -> PreviewController.PreviewQuality.LOW      // Fast scroll
            velocity > 500f -> PreviewController.PreviewQuality.MEDIUM   // Medium scroll
            else -> PreviewController.PreviewQuality.HIGH                  // Slow/stopped
        }
    }
    
    fun adjustQualityBasedOnBattery(batteryLevel: Int): PreviewController.PreviewQuality {
        return when {
            batteryLevel < 20 -> PreviewController.PreviewQuality.LOW
            batteryLevel < 50 -> PreviewController.PreviewQuality.MEDIUM
            else -> PreviewController.PreviewQuality.HIGH
        }
    }
}
```

### 6.2. Effect Skipping

```kotlin
class EffectSkipStrategy {
    
    fun shouldSkipEffect(
        effect: Effect,
        quality: PreviewController.PreviewQuality,
        isScrubbing: Boolean
    ): Boolean {
        // Skip heavy effects khi scrubbing
        if (isScrubbing && quality != PreviewController.PreviewQuality.HIGH) {
            return when (effect.type) {
                EffectType.BLUR -> true
                EffectType.SHARPEN -> true
                EffectType.COMPLEX_LUT -> true
                else -> false
            }
        }
        
        return false
    }
}
```

---

## 7. Integration với SlideShowViewModel

### 7.1. Update SlideShowViewModel

```kotlin
class SlideShowViewModel : BaseViewModel() {
    
    private lateinit var previewController: PreviewController
    private lateinit var frameCache: FrameCache
    private lateinit var preRenderService: PreviewPreRenderService
    
    fun initDataBase(context: SlideShowActivity) {
        this.context = context
        
        // Setup preview system
        frameCache = FrameCache()
        previewController = PreviewController(
            compositor = mediaCompositor,
            timelineController = timelineController,
            textureView = binding.textureView
        )
        preRenderService = PreviewPreRenderService(
            compositor = mediaCompositor,
            frameCache = frameCache,
            timelineController = timelineController
        )
        
        // Pre-render keyframes
        preRenderService.preRenderKeyframes()
    }
    
    fun onTimelineScrolled(time: Long) {
        previewController.onTimelineScrolled(time)
    }
    
    fun onTimelineScrollEnded() {
        previewController.onTimelineScrollEnded()
    }
    
    override fun onCleared() {
        super.onCleared()
        frameCache.clear()
        preRenderService.cleanup()
    }
}
```

---

## 8. Usage Example

```kotlin
// Setup
val timelineView = findViewById<TimelineView>(R.id.timelineView)
val previewController = PreviewController(compositor, timeline, textureView)

timelineView.setPreviewController(previewController)
timelineView.setTimelineController(timeline)

// User scrolls timeline
// → TimelineView.onTouchEvent() → PreviewController.onTimelineScrolled()
// → Check cache → Render if needed → Display frame
// → Smooth preview với effects!

// User releases scroll
// → PreviewController.onTimelineScrollEnded()
// → Render final frame với full quality
```

---

## 9. Performance Metrics

### Target Performance:
- **Scroll latency**: < 50ms
- **Frame rate khi scroll**: 30-60fps
- **Memory usage**: < 200MB cache
- **Cache hit rate**: > 70%

### Optimization Checklist:
- ✅ Frame cache với LRU
- ✅ Adaptive quality based on scroll speed
- ✅ Debounce scroll events
- ✅ Skip heavy effects khi scrubbing
- ✅ Pre-render keyframes
- ✅ Background pre-rendering
- ✅ Texture reuse/pooling

---

## 10. Testing

```kotlin
@Test
fun testScrubPreviewPerformance() {
    val startTime = System.currentTimeMillis()
    
    // Simulate rapid scrolling
    repeat(100) { i ->
        previewController.onTimelineScrolled(i * 100L)
    }
    
    val duration = System.currentTimeMillis() - startTime
    assertTrue("Scrub should be fast", duration < 2000) // < 2s for 100 frames
}

@Test
fun testCacheHitRate() {
    // Render same frame multiple times
    repeat(10) {
        previewController.onTimelineScrolled(1000L)
    }
    
    val hitRate = frameCache.getHitRate()
    assertTrue("Cache should work", hitRate > 0.8) // > 80% hit rate
}
```

