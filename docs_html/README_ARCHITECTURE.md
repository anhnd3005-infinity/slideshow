# 📚 Architecture Documentation Index

## 🎯 Master Document

**[MASTER_ARCHITECTURE.md](./MASTER_ARCHITECTURE.md)** - **ĐỌC FILE NÀY TRƯỚC!**

Document tổng hợp đầy đủ với:
- ✅ Luồng hoàn chỉnh từ chọn media → preview → export
- ✅ Code examples chi tiết
- ✅ Timeline visualization
- ✅ Flow diagrams

---

## 📖 Chi Tiết Các Components

### 1. [ARCHITECTURE_CAPCUT_STYLE.md](./ARCHITECTURE_CAPCUT_STYLE.md)
**Kiến trúc chi tiết multi-track timeline**

Nội dung:
- TimelineController, Track, Clip data structures
- MediaSourceManager (ExoPlayer, Bitmap)
- MultiLayerCompositor rendering pipeline
- Audio mixing system
- Effects & Filter library
- Keyframe animation system
- Export pipeline

**Đọc khi:** Bạn cần hiểu chi tiết từng component

---

### 2. [ARCHITECTURE_HUONG2.md](./ARCHITECTURE_HUONG2.md)
**Kiến trúc ExoPlayer + GL Compositing (Alternative approach)**

Nội dung:
- ExoPlayer integration với SurfaceTexture
- VideoTextureManager
- MediaScene vs Scene hiện tại
- Migration path từ VideoComposer

**Đọc khi:** Bạn muốn hiểu cách tích hợp ExoPlayer với OpenGL

---

### 3. [REALTIME_PREVIEW_EXPLAINED.md](./REALTIME_PREVIEW_EXPLAINED.md)
**Giải thích real-time preview mechanism**

Nội dung:
- Render loop hoạt động như thế nào
- Tại sao preview ngay lập tức khi apply effect
- Performance optimization
- Edge cases (heavy effects, multiple layers)

**Đọc khi:** Bạn muốn hiểu tại sao preview instant

---

### 4. [TIMELINE_LAYERS_EXPLAINED.md](./TIMELINE_LAYERS_EXPLAINED.md)
**Giải thích timeline layers theo thời gian**

Nội dung:
- Mỗi layer có startTime/duration riêng
- Query clips tại thời điểm cụ thể
- Ví dụ timeline phức tạp
- Performance optimization

**Đọc khi:** Bạn muốn hiểu cách chèn effects vào các khoảng thời gian cụ thể

---

## 🎬 Flow Diagrams

### 1. [Complete Flow Diagram](https://www.figma.com/online-whiteboard/create-diagram/5726bb1c-2529-466e-a000-28554864b1c3)
**Luồng từ chọn media → preview → export**

### 2. [CapCut-Style Architecture](https://www.figma.com/online-whiteboard/create-diagram/fd10fef3-b23a-4c19-ada6-fed3ae6a3a76)
**Kiến trúc multi-track timeline**

### 3. [ExoPlayer GL Compositing](https://www.figma.com/online-whiteboard/create-diagram/201ab8f0-06c0-490a-a668-0ccf1ea44f4e)
**Kiến trúc ExoPlayer + OpenGL**

---

## 🚀 Quick Start Guide

### Bước 1: Đọc Master Document
👉 **[MASTER_ARCHITECTURE.md](./MASTER_ARCHITECTURE.md)**

### Bước 2: Hiểu Core Concepts
- Timeline multi-track
- Clip với startTime/duration
- Real-time preview
- Export pipeline

### Bước 3: Implementation Phases
1. **Phase 1**: Core Timeline (TimelineController, Track, Clip)
2. **Phase 2**: Media Playback (ExoPlayer, Bitmap loading)
3. **Phase 3**: Rendering Pipeline (Compositor, Shaders)
4. **Phase 4**: Real-time Preview (PreviewRenderer)
5. **Phase 5**: Effects System (Filters, Transitions, Keyframes)
6. **Phase 6**: Export (VideoExporter, MediaCodec)

---

## 📋 Key Features

✅ **Mix ảnh + video** trong cùng timeline  
✅ **Multi-track**: Video, Overlay, Audio, Text  
✅ **Timeline layers**: Mỗi layer có timeline riêng  
✅ **Real-time preview**: 30-60fps với effects  
✅ **Keyframe animation**: Transform, opacity theo thời gian  
✅ **Rich effects**: Filter, transition, sticker, text  
✅ **Audio mixing**: Background music, SFX, voice-over  
✅ **High-quality export**: MediaCodec encoding  

---

## 🔍 Tìm Thông Tin Nhanh

| Câu hỏi | Document |
|---------|----------|
| Luồng từ đầu đến cuối? | MASTER_ARCHITECTURE.md |
| Timeline hoạt động thế nào? | ARCHITECTURE_CAPCUT_STYLE.md |
| Preview tại sao instant? | REALTIME_PREVIEW_EXPLAINED.md |
| Chèn effect vào khoảng thời gian? | TIMELINE_LAYERS_EXPLAINED.md |
| ExoPlayer integration? | ARCHITECTURE_HUONG2.md |

---

## 📞 Support

Nếu có câu hỏi về kiến trúc, hãy đọc:
1. **MASTER_ARCHITECTURE.md** trước
2. Sau đó đọc document cụ thể cho component bạn quan tâm

---

**Last Updated:** 2024-12-24

