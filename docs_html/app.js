// Configuration
const MARKDOWN_FILES = {
    'README_ARCHITECTURE.md': './README_ARCHITECTURE.md',
    'MASTER_ARCHITECTURE.md': './MASTER_ARCHITECTURE.md',
    'ARCHITECTURE_CAPCUT_STYLE.md': './ARCHITECTURE_CAPCUT_STYLE.md',
    'ARCHITECTURE_HUONG2.md': './ARCHITECTURE_HUONG2.md',
    'REALTIME_PREVIEW_EXPLAINED.md': './REALTIME_PREVIEW_EXPLAINED.md',
    'TIMELINE_LAYERS_EXPLAINED.md': './TIMELINE_LAYERS_EXPLAINED.md',
    'TIMELINE_SCRUB_PREVIEW.md': './TIMELINE_SCRUB_PREVIEW.md'
};

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    initializeNavigation();
    loadDefaultDocument();
});

// Initialize navigation
function initializeNavigation() {
    const navLinks = document.querySelectorAll('.nav-link[data-file]');
    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const fileName = link.getAttribute('data-file');
            loadDocument(fileName);
            
            // Update active state
            navLinks.forEach(l => l.classList.remove('active'));
            link.classList.add('active');
        });
    });
}

// Load default document
function loadDefaultDocument() {
    loadDocument('README_ARCHITECTURE.md');
}

// Load markdown document
async function loadDocument(fileName) {
    const contentDiv = document.getElementById('content');
    const titleDiv = document.getElementById('page-title');
    const breadcrumbDiv = document.getElementById('breadcrumb');
    
    // Show loading
    contentDiv.innerHTML = `
        <div class="loading">
            <div class="spinner"></div>
            <p>Loading ${fileName}...</p>
        </div>
    `;
    
    // Update title
    const displayName = fileName.replace('.md', '').replace(/_/g, ' ');
    titleDiv.textContent = displayName;
    breadcrumbDiv.textContent = `Home > ${displayName}`;
    
    try {
        const filePath = MARKDOWN_FILES[fileName];
        if (!filePath) {
            throw new Error(`File ${fileName} not found in configuration`);
        }
        
        const response = await fetch(filePath);
        if (!response.ok) {
            throw new Error(`Failed to load ${fileName}: ${response.statusText}`);
        }
        
        const markdown = await response.text();
        const html = marked.parse(markdown, {
            breaks: true,
            gfm: true,
            highlight: function(code, lang) {
                if (lang && hljs.getLanguage(lang)) {
                    try {
                        return hljs.highlight(code, { language: lang }).value;
                    } catch (err) {
                        console.error('Highlight error:', err);
                    }
                }
                return hljs.highlightAuto(code).value;
            }
        });
        
        // Render content
        contentDiv.innerHTML = `<div class="markdown-content">${html}</div>`;
        
        // Process code blocks
        contentDiv.querySelectorAll('pre code').forEach((block) => {
            hljs.highlightElement(block);
        });
        
        // Process internal links
        processInternalLinks(contentDiv);
        
        // Scroll to top
        window.scrollTo(0, 0);
        
    } catch (error) {
        console.error('Error loading document:', error);
        contentDiv.innerHTML = `
            <div class="error">
                <h3>❌ Error Loading Document</h3>
                <p>${error.message}</p>
                <p>Please make sure the markdown file exists at: <code>${MARKDOWN_FILES[fileName]}</code></p>
            </div>
        `;
    }
}

// Process internal links in markdown
function processInternalLinks(container) {
    const links = container.querySelectorAll('a[href$=".md"]');
    links.forEach(link => {
        const href = link.getAttribute('href');
        const fileName = href.split('/').pop();
        
        if (MARKDOWN_FILES[fileName]) {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                loadDocument(fileName);
                
                // Update active nav link
                const navLinks = document.querySelectorAll('.nav-link[data-file]');
                navLinks.forEach(l => {
                    if (l.getAttribute('data-file') === fileName) {
                        navLinks.forEach(nl => nl.classList.remove('active'));
                        l.classList.add('active');
                    }
                });
            });
        }
    });
}

// Configure marked.js
marked.setOptions({
    breaks: true,
    gfm: true,
    headerIds: true,
    mangle: false
});

