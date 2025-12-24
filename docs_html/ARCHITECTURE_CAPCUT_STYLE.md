# Kiến Trúc Video Editor Kiểu CapCut

## Tổng Quan

Hệ thống video editor đa lớp với timeline multi-track, hỗ trợ:
- **Multi-track timeline**: Video, Overlay, Audio, Text tracks
- **Keyframe animation**: Transform, opacity, effects theo thời gian
- **Rich effects**: Filter, transition, sticker, text với animation
- **Audio mixing**: Background music, sound effects, voice-over
- **Real-time preview**: Smooth playback với effects
- **High-quality export**: Multi-pass rendering với MediaCodec

---

## 1. Timeline Structure

### 1.1. Timeline Controller

```kotlin
class TimelineController {
    private val tracks = mutableMapOf<TrackType, Track>()
    
    enum class TrackType {
        VIDEO,      // Video/Image clips
        OVERLAY,    // Stickers, frames, effects
        AUDIO,      // Background music, SFX
        TEXT        // Text layers
    }
    
    var currentTime: Long = 0L
    var totalDuration: Long = 0L
    var playbackSpeed: Float = 1.0f
    
    fun addTrack(type: TrackType): Track {
        val track = Track(type)
        tracks[type] = track
        return track
    }
    
    fun getTrack(type: TrackType): Track? = tracks[type]
    
    fun getClipsAtTime(time: Long): Map<TrackType, List<Clip>> {
        return tracks.mapValues { (_, track) ->
            track.getClipsAtTime(time)
        }
    }
    
    fun evaluateDuration() {
        totalDuration = tracks.values.maxOfOrNull { it.getEndTime() } ?: 0L
    }
}
```

### 1.2. Track & Clip

```kotlin
data class Track(
    val type: TrackType,
    val clips: MutableList<Clip> = mutableListOf(),
    val locked: Boolean = false,
    val muted: Boolean = false
) {
    fun addClip(clip: Clip) {
        clips.add(clip)
        clips.sortBy { it.startTime }
    }
    
    fun getClipsAtTime(time: Long): List<Clip> {
        return clips.filter { 
            time >= it.startTime && time < it.endTime 
        }
    }
    
    fun getEndTime(): Long {
        return clips.maxOfOrNull { it.endTime } ?: 0L
    }
}

data class Clip(
    val id: String = UUID.randomUUID().toString(),
    val type: ClipType,
    val source: MediaSource,  // Uri, path, etc.
    
    // Timeline
    var startTime: Long = 0L,
    var duration: Long = 3000L,
    val endTime: Long get() = startTime + duration,
    
    // Transform (có thể animate bằng keyframes)
    var transform: Transform = Transform(),
    
    // Effects
    var filter: Filter? = null,
    var transitionIn: Transition? = null,
    var transitionOut: Transition? = null,
    
    // Keyframes cho animation
    val keyframes: MutableList<Keyframe> = mutableListOf(),
    
    // Speed control
    var speed: Float = 1.0f,  // 0.25x, 0.5x, 1x, 2x, 4x
    
    // Crop
    var crop: CropRect? = null,
    
    // Audio (cho video clips)
    var volume: Float = 1.0f,
    var audioFadeIn: Long = 0L,
    var audioFadeOut: Long = 0L
)

enum class ClipType {
    VIDEO,
    IMAGE,
    AUDIO,
    STICKER,
    TEXT,
    EFFECT_OVERLAY
}

data class Transform(
    var position: PointF = PointF(0.5f, 0.5f),  // normalized 0-1
    var scale: Float = 1.0f,
    var rotation: Float = 0f,
    var opacity: Float = 1.0f
)

data class Keyframe(
    val time: Long,  // relative to clip start
    val transform: Transform,
    val easing: EasingType = EasingType.LINEAR
)

enum class EasingType {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    BOUNCE,
    ELASTIC
}
```

---

## 2. Media Sources & Players

### 2.1. Media Source Manager

```kotlin
class MediaSourceManager(private val context: Context) {
    
    // ExoPlayer pool cho video clips
    private val videoPlayers = mutableMapOf<String, ExoPlayer>()
    private val videoTextures = mutableMapOf<String, VideoTexture>()
    
    // Bitmap cache cho images
    private val imageCache = LruCache<String, Bitmap>(50)
    
    // Audio players
    private val audioPlayers = mutableMapOf<String, ExoPlayer>()
    
    data class VideoTexture(
        val player: ExoPlayer,
        val surfaceTexture: SurfaceTexture,
        val texture: Texture2d  // OES texture
    )
    
    fun setupVideoClip(clip: Clip): VideoTexture {
        val player = ExoPlayer.Builder(context).build()
        val surfaceTexture = SurfaceTexture(0)
        val texture = Texture2d()
        
        texture.initialize()
        surfaceTexture.attachToGLContext(texture.id)
        
        player.setVideoSurface(Surface(surfaceTexture))
        player.setMediaItem(MediaItem.fromUri(clip.source.uri))
        player.prepare()
        
        // Handle speed
        player.playbackParameters = PlaybackParameters(clip.speed)
        
        videoPlayers[clip.id] = player
        val videoTexture = VideoTexture(player, surfaceTexture, texture)
        videoTextures[clip.id] = videoTexture
        
        return videoTexture
    }
    
    fun updateVideoTexture(clip: Clip, time: Long) {
        val videoTexture = videoTextures[clip.id] ?: return
        val player = videoTexture.player
        
        // Calculate actual video time considering speed
        val videoTime = (time - clip.startTime) * clip.speed
        
        // Handle looping if needed
        val videoDuration = player.duration
        if (videoDuration > 0 && videoTime >= videoDuration) {
            player.seekTo(videoTime % videoDuration)
        } else {
            player.seekTo(videoTime)
        }
        
        videoTexture.surfaceTexture.updateTexImage()
    }
    
    fun loadImage(source: MediaSource): Bitmap {
        val cacheKey = source.uri.toString()
        return imageCache.get(cacheKey) ?: run {
            val bitmap = loadBitmapFromUri(context, source.uri)
            imageCache.put(cacheKey, bitmap)
            bitmap
        }
    }
}
```

---

## 3. Rendering Pipeline

### 3.1. Multi-Layer Compositor

```kotlin
class MultiLayerCompositor(
    private val context: Context,
    private val glContext: EGLContext,
    private val timelineController: TimelineController,
    private val mediaSourceManager: MediaSourceManager
) {
    
    // Shaders
    private val filterShader = FilterShader()
    private val transformShader = TransformShader()
    private val blendShader = BlendShader()
    private val textShader = TextShader()
    
    // FrameBuffers
    private val layerFBOs = mutableListOf<FrameBuffer>()
    private val finalFBO = FrameBuffer()
    
    fun renderFrame(time: Long, width: Int, height: Int): Texture2d {
        val clipsAtTime = timelineController.getClipsAtTime(time)
        
        // Clear final buffer
        finalFBO.bind()
        glClear(GL_COLOR_BUFFER_BIT)
        
        // Render video/image track (base layer)
        renderVideoTrack(clipsAtTime[TrackType.VIDEO] ?: emptyList(), time, width, height)
        
        // Render overlay track
        renderOverlayTrack(clipsAtTime[TrackType.OVERLAY] ?: emptyList(), time, width, height)
        
        // Render text track
        renderTextTrack(clipsAtTime[TrackType.TEXT] ?: emptyList(), time, width, height)
        
        return finalFBO.texture
    }
    
    private fun renderVideoTrack(clips: List<Clip>, time: Long, width: Int, height: Int) {
        clips.forEach { clip ->
            val texture = when (clip.type) {
                ClipType.VIDEO -> {
                    // Update video texture
                    mediaSourceManager.updateVideoTexture(clip, time)
                    mediaSourceManager.videoTextures[clip.id]?.texture
                }
                ClipType.IMAGE -> {
                    // Load image texture
                    val bitmap = mediaSourceManager.loadImage(clip.source)
                    bitmapToTexture(bitmap)
                }
                else -> null
            } ?: return@forEach
            
            // Calculate transform with keyframes
            val transform = calculateTransformAtTime(clip, time)
            
            // Apply crop if needed
            val croppedTexture = if (clip.crop != null) {
                applyCrop(texture, clip.crop!!)
            } else {
                texture
            }
            
            // Apply filter
            val filteredTexture = if (clip.filter != null) {
                applyFilter(croppedTexture, clip.filter!!)
            } else {
                croppedTexture
            }
            
            // Apply transform (position, scale, rotation, opacity)
            transformShader.use {
                transformShader.setTexture(filteredTexture)
                transformShader.setTransform(transform)
                transformShader.setOpacity(transform.opacity)
                // Render to layer FBO
            }
            
            // Blend onto final FBO
            blendShader.use {
                blendShader.setSourceTexture(layerFBO.texture)
                blendShader.setDestinationTexture(finalFBO.texture)
                blendShader.setBlendMode(BlendMode.NORMAL)
                // Render to finalFBO
            }
        }
    }
    
    private fun calculateTransformAtTime(clip: Clip, time: Long): Transform {
        val relativeTime = time - clip.startTime
        
        if (clip.keyframes.isEmpty()) {
            return clip.transform
        }
        
        // Find surrounding keyframes
        val before = clip.keyframes.lastOrNull { it.time <= relativeTime }
        val after = clip.keyframes.firstOrNull { it.time > relativeTime }
        
        if (before == null) return clip.transform
        if (after == null) return before.transform
        
        // Interpolate
        val t = (relativeTime - before.time).toFloat() / (after.time - before.time).toFloat()
        val easedT = applyEasing(t, after.easing)
        
        return Transform(
            position = lerp(before.transform.position, after.transform.position, easedT),
            scale = lerp(before.transform.scale, after.transform.scale, easedT),
            rotation = lerp(before.transform.rotation, after.transform.rotation, easedT),
            opacity = lerp(before.transform.opacity, after.transform.opacity, easedT)
        )
    }
    
    private fun renderOverlayTrack(clips: List<Clip>, time: Long, width: Int, height: Int) {
        clips.sortedBy { it.transform.position.y }.forEach { clip ->
            when (clip.type) {
                ClipType.STICKER -> renderSticker(clip, time)
                ClipType.EFFECT_OVERLAY -> renderEffectOverlay(clip, time)
                else -> {}
            }
        }
    }
    
    private fun renderTextTrack(clips: List<Clip>, time: Long, width: Int, height: Int) {
        clips.forEach { clip ->
            if (clip.type == ClipType.TEXT) {
                renderText(clip, time)
            }
        }
    }
}
```

---

## 4. Audio Mixing

### 4.1. Audio Mixer

```kotlin
class AudioMixer(
    private val context: Context,
    private val timelineController: TimelineController
) {
    private val audioTracks = mutableListOf<AudioTrack>()
    
    fun prepareAudio(time: Long, duration: Long): AudioBuffer {
        val clips = timelineController.getClipsAtTime(time)
        val audioClips = clips[TrackType.AUDIO] ?: emptyList()
        
        // Mix all audio clips at this time
        val mixedBuffer = AudioBuffer()
        
        audioClips.forEach { clip ->
            val audioData = getAudioData(clip, time)
            val volume = calculateVolume(clip, time)
            
            // Apply fade in/out
            val fadeMultiplier = calculateFade(clip, time)
            val finalVolume = volume * fadeMultiplier
            
            // Mix into buffer
            mixAudio(mixedBuffer, audioData, finalVolume)
        }
        
        return mixedBuffer
    }
    
    private fun calculateVolume(clip: Clip, time: Long): Float {
        val relativeTime = time - clip.startTime
        
        // Check keyframes for volume animation
        val volumeKeyframes = clip.keyframes.filter { 
            it.transform.opacity != clip.transform.opacity 
        }
        
        if (volumeKeyframes.isEmpty()) {
            return clip.volume
        }
        
        // Interpolate volume from keyframes
        // Similar to transform interpolation
        return clip.volume
    }
    
    private fun calculateFade(clip: Clip, time: Long): Float {
        val relativeTime = time - clip.startTime
        val duration = clip.duration
        
        var fadeMultiplier = 1.0f
        
        // Fade in
        if (relativeTime < clip.audioFadeIn) {
            fadeMultiplier = relativeTime.toFloat() / clip.audioFadeIn.toFloat()
        }
        
        // Fade out
        if (relativeTime > duration - clip.audioFadeOut) {
            val fadeOutStart = duration - clip.audioFadeOut
            val fadeOutProgress = (relativeTime - fadeOutStart) / clip.audioFadeOut.toFloat()
            fadeMultiplier = 1.0f - fadeOutProgress
        }
        
        return fadeMultiplier.coerceIn(0f, 1f)
    }
}
```

---

## 5. Effects System

### 5.1. Filter Library

```kotlin
class FilterLibrary {
    
    enum class FilterType {
        COLOR_GRADE,
        LUT,
        TONE_CURVE,
        BLUR,
        SHARPEN,
        VIGNETTE,
        GRAIN,
        CUSTOM
    }
    
    data class Filter(
        val id: String,
        val type: FilterType,
        val name: String,
        val shader: String,  // GLSL shader code
        val parameters: Map<String, Float> = emptyMap()
    )
    
    companion object {
        val PRESETS = listOf(
            Filter("vivid", FilterType.COLOR_GRADE, "Vivid", VIVID_SHADER, mapOf("saturation" to 1.2f)),
            Filter("cinematic", FilterType.LUT, "Cinematic", LUT_SHADER, mapOf("lut_strength" to 0.8f)),
            Filter("black_white", FilterType.COLOR_GRADE, "B&W", BW_SHADER),
            // ... more presets
        )
    }
}

class TransitionLibrary {
    
    enum class TransitionType {
        FADE,
        SLIDE,
        ZOOM,
        ROTATE,
        WIPE,
        CUSTOM
    }
    
    data class Transition(
        val id: String,
        val type: TransitionType,
        val name: String,
        val duration: Long = 1000L,
        val shader: String  // GLSL shader
    )
    
    companion object {
        val PRESETS = listOf(
            Transition("fade", TransitionType.FADE, "Fade", 1000L, FADE_SHADER),
            Transition("slide_left", TransitionType.SLIDE, "Slide Left", 800L, SLIDE_SHADER),
            Transition("zoom_in", TransitionType.ZOOM, "Zoom In", 1200L, ZOOM_SHADER),
            // ... more transitions
        )
    }
}
```

---

## 6. Keyframe Animation System

### 6.1. Keyframe Editor

```kotlin
class KeyframeEditor {
    
    fun addKeyframe(clip: Clip, time: Long, transform: Transform) {
        val keyframe = Keyframe(
            time = time - clip.startTime,  // relative to clip
            transform = transform
        )
        
        clip.keyframes.add(keyframe)
        clip.keyframes.sortBy { it.time }
    }
    
    fun removeKeyframe(clip: Clip, keyframeId: String) {
        clip.keyframes.removeAll { it.id == keyframeId }
    }
    
    fun interpolateKeyframes(
        clip: Clip,
        time: Long
    ): Transform {
        val relativeTime = time - clip.startTime
        
        if (clip.keyframes.isEmpty()) {
            return clip.transform
        }
        
        // Find surrounding keyframes
        val before = clip.keyframes.lastOrNull { it.time <= relativeTime }
        val after = clip.keyframes.firstOrNull { it.time > relativeTime }
        
        if (before == null) return clip.transform
        if (after == null) return before.transform
        
        // Calculate interpolation factor
        val t = (relativeTime - before.time).toFloat() / 
                (after.time - before.time).toFloat()
        
        // Apply easing
        val easedT = applyEasing(t, after.easing)
        
        // Interpolate each property
        return Transform(
            position = lerp(before.transform.position, after.transform.position, easedT),
            scale = lerp(before.transform.scale, after.transform.scale, easedT),
            rotation = lerpAngle(before.transform.rotation, after.transform.rotation, easedT),
            opacity = lerp(before.transform.opacity, after.transform.opacity, easedT)
        )
    }
    
    private fun applyEasing(t: Float, easing: EasingType): Float {
        return when (easing) {
            EasingType.LINEAR -> t
            EasingType.EASE_IN -> t * t
            EasingType.EASE_OUT -> 1 - (1 - t) * (1 - t)
            EasingType.EASE_IN_OUT -> {
                if (t < 0.5f) 2 * t * t
                else 1 - 2 * (1 - t) * (1 - t)
            }
            EasingType.BOUNCE -> bounceEasing(t)
            EasingType.ELASTIC -> elasticEasing(t)
        }
    }
}
```

---

## 7. Export Pipeline

### 7.1. Video Exporter

```kotlin
class VideoExporter(
    private val context: Context,
    private val timelineController: TimelineController,
    private val compositor: MultiLayerCompositor,
    private val audioMixer: AudioMixer
) {
    
    fun export(
        outputPath: String,
        width: Int = 1080,
        height: Int = 1920,
        fps: Int = 30,
        bitrate: Int = 8000000,
        onProgress: (Float) -> Unit
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
            
            // Render frame
            val frameTexture = compositor.renderFrame(time, width, height)
            
            // Encode video frame
            encodeVideoFrame(videoEncoder, frameTexture, frameIndex, fps)
            
            // Encode audio (if needed)
            if (frameIndex % fps == 0) {  // Once per second
                val audioBuffer = audioMixer.prepareAudio(time, 1000L)
                encodeAudioFrame(audioEncoder, audioBuffer)
            }
        }
        
        // Finish encoding
        finishEncoding(videoEncoder, audioEncoder, muxer)
    }
    
    private fun setupVideoEncoder(
        width: Int,
        height: Int,
        fps: Int,
        bitrate: Int
    ): MediaCodec {
        val format = MediaFormat.createVideoFormat("video/avc", width, height)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, 
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        
        val encoder = MediaCodec.createEncoderByType("video/avc")
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        
        return encoder
    }
}
```

---

## 8. UI Integration

### 8.1. Timeline View

```kotlin
class TimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    
    private var timelineController: TimelineController? = null
    private var pixelsPerSecond = 50f
    private var trackHeight = 80f
    
    fun setTimelineController(controller: TimelineController) {
        this.timelineController = controller
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        val controller = timelineController ?: return
        
        // Draw tracks
        var y = 0f
        TrackType.values().forEach { trackType ->
            val track = controller.getTrack(trackType) ?: return@forEach
            
            // Draw track background
            canvas.drawRect(0f, y, width.toFloat(), y + trackHeight, trackPaint)
            
            // Draw clips in track
            track.clips.forEach { clip ->
                val x = clip.startTime / 1000f * pixelsPerSecond
                val clipWidth = clip.duration / 1000f * pixelsPerSecond
                
                // Draw clip rectangle
                canvas.drawRect(x, y, x + clipWidth, y + trackHeight, clipPaint)
                
                // Draw clip name/thumbnail
                drawClipContent(canvas, clip, x, y, clipWidth, trackHeight)
            }
            
            y += trackHeight
        }
        
        // Draw playhead
        val playheadX = controller.currentTime / 1000f * pixelsPerSecond
        canvas.drawLine(playheadX, 0f, playheadX, height.toFloat(), playheadPaint)
    }
}
```

---

## 9. Feature Comparison với CapCut

| Feature | CapCut | Our Architecture |
|---------|--------|-----------------|
| Multi-track timeline | ✅ | ✅ |
| Video + Image mixing | ✅ | ✅ |
| Keyframe animation | ✅ | ✅ |
| Filters & Effects | ✅ | ✅ |
| Transitions | ✅ | ✅ |
| Audio mixing | ✅ | ✅ |
| Text layers | ✅ | ✅ |
| Stickers/Overlays | ✅ | ✅ |
| Speed control | ✅ | ✅ |
| Crop & Transform | ✅ | ✅ |
| Real-time preview | ✅ | ✅ |
| Export HD/4K | ✅ | ✅ |
| AI features | ✅ | ❌ (future) |

---

## 10. Implementation Phases

### Phase 1: Core Timeline (Week 1-2)
- [ ] TimelineController
- [ ] Track & Clip data structures
- [ ] Basic UI timeline view

### Phase 2: Media Playback (Week 3-4)
- [ ] ExoPlayer integration
- [ ] Video texture management
- [ ] Image loading & caching

### Phase 3: Rendering Pipeline (Week 5-7)
- [ ] MultiLayerCompositor
- [ ] Basic transform (position, scale, rotation)
- [ ] Filter system
- [ ] Transition system

### Phase 4: Advanced Features (Week 8-10)
- [ ] Keyframe animation
- [ ] Audio mixing
- [ ] Text rendering
- [ ] Sticker/overlay system

### Phase 5: Export (Week 11-12)
- [ ] Video encoder
- [ ] Audio encoder
- [ ] MediaMuxer integration
- [ ] Progress tracking

### Phase 6: Polish (Week 13-14)
- [ ] Performance optimization
- [ ] Memory management
- [ ] UI/UX improvements
- [ ] Bug fixes

---

## 11. Performance Considerations

### Memory Management
- **Texture pooling**: Reuse textures instead of creating new ones
- **LruCache**: Cache decoded images/videos
- **Lazy loading**: Load media only when needed
- **Release unused**: Properly release ExoPlayer instances

### Rendering Optimization
- **FBO reuse**: Reuse framebuffers across frames
- **Shader caching**: Cache compiled shaders
- **Batch rendering**: Group similar operations
- **LOD system**: Lower quality for preview, high quality for export

### Timeline Optimization
- **Spatial indexing**: Quick lookup of clips at specific time
- **Incremental updates**: Only re-render changed areas
- **Background processing**: Pre-process effects in background

---

## 12. Example Usage

```kotlin
// Setup timeline
val timeline = TimelineController()
val videoTrack = timeline.addTrack(TrackType.VIDEO)
val audioTrack = timeline.addTrack(TrackType.AUDIO)

// Add video clip
val videoClip = Clip(
    type = ClipType.VIDEO,
    source = MediaSource(uri = videoUri),
    startTime = 0L,
    duration = 5000L,
    filter = FilterLibrary.PRESETS.find { it.id == "cinematic" },
    transform = Transform(
        position = PointF(0.5f, 0.5f),
        scale = 1.0f
    )
)
videoTrack.addClip(videoClip)

// Add image clip with transition
val imageClip = Clip(
    type = ClipType.IMAGE,
    source = MediaSource(uri = imageUri),
    startTime = 5000L,
    duration = 3000L,
    transitionIn = TransitionLibrary.PRESETS.find { it.id == "fade" }
)
videoTrack.addClip(imageClip)

// Add keyframe animation
val keyframeEditor = KeyframeEditor()
keyframeEditor.addKeyframe(videoClip, 0L, Transform(scale = 1.0f))
keyframeEditor.addKeyframe(videoClip, 5000L, Transform(scale = 1.5f, 
    easing = EasingType.EASE_IN_OUT))

// Add audio
val audioClip = Clip(
    type = ClipType.AUDIO,
    source = MediaSource(uri = audioUri),
    startTime = 0L,
    duration = 8000L,
    volume = 0.7f
)
audioTrack.addClip(audioClip)

// Setup compositor
val compositor = MultiLayerCompositor(context, glContext, timeline, mediaSourceManager)

// Preview
compositor.renderFrame(2500L, 1080, 1920) // Render at 2.5s

// Export
val exporter = VideoExporter(context, timeline, compositor, audioMixer)
exporter.export(
    outputPath = "/path/to/output.mp4",
    width = 1080,
    height = 1920,
    fps = 30,
    onProgress = { progress ->
        Log.d("Export", "Progress: ${progress * 100}%")
    }
)
```

