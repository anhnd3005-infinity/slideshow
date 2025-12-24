# ⚡ Quick Start Guide

## 🚀 Mở Documentation Viewer

### Cách Nhanh Nhất:

```bash
cd docs_html
python3 -m http.server 8000
```

Sau đó mở browser: **http://localhost:8000**

---

## 📋 Các Bước Chi Tiết

### 1. Sync Markdown Files (nếu chưa sync)

```bash
cd docs_html
./sync_markdown.sh
```

### 2. Start Server

**Option A: Python**
```bash
python3 -m http.server 8000
```

**Option B: Node.js**
```bash
npx http-server -p 8000
```

**Option C: PHP**
```bash
php -S localhost:8000
```

### 3. Mở Browser

- **http://localhost:8000**
- Click vào các link trong sidebar để xem documents

---

## 📚 Documents Có Sẵn

1. **📋 Index & Navigation** - Tổng quan và navigation guide
2. **🎬 Master Architecture** - Luồng đầy đủ từ đầu đến cuối
3. **🎨 CapCut-Style Architecture** - Kiến trúc chi tiết
4. **🔄 ExoPlayer GL Compositing** - ExoPlayer integration
5. **⚡ Real-time Preview** - Giải thích preview mechanism
6. **📊 Timeline Layers** - Timeline layers theo thời gian
7. **🎯 Timeline Scrub Preview** - Scrub preview system

---

## 💡 Tips

- **Search**: Dùng Ctrl+F (Cmd+F) để search trong document
- **Navigation**: Click vào link trong sidebar hoặc trong document
- **External Links**: Links đến Figma diagrams sẽ mở tab mới

---

## ❓ Troubleshooting

**Lỗi CORS?** → Dùng local server, không mở file trực tiếp

**File không load?** → Chạy `./sync_markdown.sh` để sync files

**Syntax highlighting không hoạt động?** → Cần internet để load highlight.js từ CDN

