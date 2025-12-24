# 🚀 Deploy Guide - GitHub + Vercel

## 📋 Bước 1: Push lên GitHub

### 1.1. Setup GitHub Remote (nếu chưa có)

```bash
# Kiểm tra remote hiện tại
git remote -v

# Thêm GitHub remote (thay YOUR_USERNAME và YOUR_REPO)
git remote add github https://github.com/YOUR_USERNAME/YOUR_REPO.git

# Hoặc nếu muốn thay thế origin
git remote set-url origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
```

### 1.2. Add và Commit Files

```bash
# Add tất cả files mới
git add .

# Hoặc add từng phần
git add docs_html/
git add *.md
git add app/src/main/java/com/ynsuper/slideshowver1/util/PermissionHelper.kt

# Commit
git commit -m "feat: Add architecture documentation and HTML viewer

- Add multi-track timeline architecture docs
- Add ExoPlayer GL compositing architecture
- Add real-time preview explanation
- Add timeline layers documentation
- Add HTML documentation viewer with dark theme
- Add PermissionHelper utility
- Fix TedImagePicker callback issue
- Support image + video mixing in slideshow"

# Push lên GitHub
git push github master
# hoặc
git push github main
```

---

## 🌐 Bước 2: Deploy lên Vercel

### Option 1: Deploy qua Vercel Dashboard (Recommended)

1. **Truy cập**: https://vercel.com
2. **Login** với GitHub account
3. **Import Project**:
   - Click "Add New Project"
   - Chọn repository từ GitHub
   - **Root Directory**: Chọn `docs_html`
   - **Framework Preset**: Other
   - **Build Command**: (để trống hoặc `echo 'No build'`)
   - **Output Directory**: `.` (current directory)
4. **Deploy**

### Option 2: Deploy qua Vercel CLI

```bash
# Install Vercel CLI
npm i -g vercel

# Login
vercel login

# Deploy
cd docs_html
vercel

# Follow prompts:
# - Set up and deploy? Yes
# - Which scope? (chọn account)
# - Link to existing project? No
# - Project name? architecture-docs (hoặc tên bạn muốn)
# - Directory? ./
# - Override settings? No
```

### Option 3: Deploy từ GitHub (Auto Deploy)

1. **Connect GitHub repo** trong Vercel Dashboard
2. **Set Root Directory** = `docs_html`
3. **Auto-deploy** mỗi khi push code

---

## ⚙️ Vercel Configuration

File `docs_html/vercel.json` đã được tạo với config:

```json
{
  "version": 2,
  "buildCommand": "echo 'No build needed'",
  "outputDirectory": ".",
  "routes": [
    {
      "src": "/(.*\\.md)",
      "headers": {
        "Content-Type": "text/markdown; charset=utf-8"
      }
    },
    {
      "src": "/(.*)",
      "dest": "/$1"
    }
  ]
}
```

---

## 🔄 Auto Sync Markdown Files

### Setup GitHub Actions (Optional)

Tạo `.github/workflows/sync-docs.yml`:

```yaml
name: Sync Documentation

on:
  push:
    paths:
      - '*.md'
      - 'docs_html/**'

jobs:
  sync:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Sync markdown files
        run: |
          cd docs_html
          ./sync_markdown.sh
      - name: Commit changes
        run: |
          git config --local user.email "action@github.com"
          git config --local user.name "GitHub Action"
          git add docs_html/*.md
          git diff --staged --quiet || git commit -m "docs: Sync markdown files"
          git push
```

---

## 📝 Checklist Trước Khi Deploy

- [ ] ✅ Đã sync markdown files vào `docs_html/`
- [ ] ✅ Đã test local (`python3 -m http.server 8000`)
- [ ] ✅ Đã commit và push lên GitHub
- [ ] ✅ Đã setup Vercel project
- [ ] ✅ Đã set Root Directory = `docs_html`
- [ ] ✅ Đã verify deployment thành công

---

## 🔗 Sau Khi Deploy

Vercel sẽ cung cấp URL như:
- `https://your-project.vercel.app`
- `https://your-project-username.vercel.app`

Bạn có thể:
- ✅ Share URL với team
- ✅ Add custom domain trong Vercel settings
- ✅ Enable auto-deploy từ GitHub

---

## 🐛 Troubleshooting

### Vercel không load markdown files

**Vấn đề**: Markdown files không được serve đúng

**Giải pháp**: 
- Kiểm tra `vercel.json` có đúng không
- Đảm bảo markdown files có trong `docs_html/`
- Check Vercel build logs

### CORS Error

**Vấn đề**: Browser block load markdown

**Giải pháp**: 
- Vercel đã handle CORS tự động
- Nếu vẫn lỗi, check `vercel.json` headers

### 404 Not Found

**Vấn đề**: Routes không match

**Giải pháp**:
- Check `vercel.json` routes config
- Đảm bảo `index.html` có trong root của `docs_html`

---

## 📞 Support

Nếu có vấn đề:
1. Check Vercel build logs
2. Check browser console
3. Verify markdown files được sync đúng

