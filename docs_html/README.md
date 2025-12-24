# 📚 Architecture Documentation Viewer

## 🚀 Cách Sử Dụng

### Quick Start

1. **Sync markdown files** (nếu chưa sync):
```bash
cd docs_html
./sync_markdown.sh
```

2. **Mở trong browser**:
   - **Option 1**: Double-click `index.html` (có thể có CORS issue)
   - **Option 2**: Dùng local server (Recommended)

### Option 1: Mở trực tiếp

```bash
# Mac
open index.html

# Linux
xdg-open index.html

# Windows
start index.html
```

⚠️ **Lưu ý**: Có thể gặp lỗi CORS khi load markdown files. Nếu vậy, dùng Option 2.

### Option 2: Dùng Local Server (Recommended)

```bash
# Python 3
cd docs_html
python3 -m http.server 8000
# Mở: http://localhost:8000

# Hoặc Node.js
npx http-server -p 8000

# Hoặc PHP
php -S localhost:8000
```

### Option 3: Dùng npm scripts

```bash
cd docs_html
npm run sync      # Sync markdown files
npm run serve     # Start Python server
npm run serve-node # Start Node.js server
npm run open      # Open in browser
```

## 📁 Cấu Trúc

```
docs_html/
├── index.html              # Main HTML file
├── styles.css              # Stylesheet (dark theme)
├── app.js                  # JavaScript logic
├── sync_markdown.sh        # Script để sync markdown files
├── package.json            # npm scripts
├── README.md               # This file
│
├── README_ARCHITECTURE.md  # Index & Navigation
├── MASTER_ARCHITECTURE.md  # Master document (luồng đầy đủ)
├── ARCHITECTURE_CAPCUT_STYLE.md  # CapCut-style architecture
├── ARCHITECTURE_HUONG2.md  # ExoPlayer GL compositing
├── REALTIME_PREVIEW_EXPLAINED.md  # Real-time preview
├── TIMELINE_LAYERS_EXPLAINED.md   # Timeline layers
└── TIMELINE_SCRUB_PREVIEW.md      # Timeline scrub preview
```

## 🔧 Configuration

### Thêm file markdown mới

1. **Sync file vào docs_html**:
```bash
./sync_markdown.sh
```

2. **Thêm vào app.js**:
```javascript
const MARKDOWN_FILES = {
    'YOUR_FILE.md': './YOUR_FILE.md',
    // ...
};
```

3. **Thêm link vào index.html sidebar**:
```html
<li><a href="#" data-file="YOUR_FILE.md" class="nav-link">Your File</a></li>
```

## 📝 Features

- ✅ **Dark theme** - Dễ đọc, không chói mắt
- ✅ **Syntax highlighting** - Code blocks với highlight.js
- ✅ **Navigation sidebar** - Dễ navigate giữa các documents
- ✅ **Responsive design** - Hoạt động trên mobile
- ✅ **Internal link processing** - Click link trong markdown để navigate
- ✅ **Auto-scroll to top** - Tự động scroll khi chuyển document
- ✅ **Breadcrumb** - Hiển thị vị trí hiện tại

## 🎨 Documents Included

### 📖 Master Documents
- **README_ARCHITECTURE.md** - Index & Navigation guide
- **MASTER_ARCHITECTURE.md** - Complete flow từ chọn media → export

### 🏗️ Architecture Details
- **ARCHITECTURE_CAPCUT_STYLE.md** - Multi-track timeline architecture
- **ARCHITECTURE_HUONG2.md** - ExoPlayer + GL compositing approach

### ⚡ Features Explained
- **REALTIME_PREVIEW_EXPLAINED.md** - Tại sao preview instant
- **TIMELINE_LAYERS_EXPLAINED.md** - Timeline layers theo thời gian
- **TIMELINE_SCRUB_PREVIEW.md** - Scrub preview system

## 🌐 Browser Support

- ✅ Chrome/Edge (latest)
- ✅ Firefox (latest)
- ✅ Safari (latest)
- ✅ Mobile browsers

## 🔄 Sync Markdown Files

Khi bạn update markdown files trong parent directory, sync lại:

```bash
cd docs_html
./sync_markdown.sh
```

Hoặc tự động sync khi start server (có thể thêm vào script).

## 📞 Troubleshooting

### CORS Error
**Vấn đề**: Browser block load markdown files do CORS policy

**Giải pháp**: Dùng local server thay vì mở file trực tiếp:
```bash
python3 -m http.server 8000
```

### File Not Found
**Vấn đề**: Không tìm thấy markdown file

**Giải pháp**: 
1. Chạy `./sync_markdown.sh` để sync files
2. Kiểm tra file có trong `docs_html/` không
3. Kiểm tra path trong `app.js` có đúng không

### Syntax Highlighting Không Hoạt Động
**Vấn đề**: Code blocks không có màu

**Giải pháp**: 
- Kiểm tra internet connection (highlight.js load từ CDN)
- Hoặc download highlight.js về local

## 🚀 Tips

- **Bookmark**: Bookmark `http://localhost:8000` để dễ truy cập
- **Search**: Dùng Ctrl+F (Cmd+F trên Mac) để search trong document
- **Print**: Có thể print document (Ctrl+P / Cmd+P)


