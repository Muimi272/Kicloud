(function () {
    const STORAGE_KEY = 'kicloud-theme';
    const root = document.documentElement;
    const THEME_ICONS = {
        light: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M12 3.5v2.2M12 18.3v2.2M5.99 5.99l1.56 1.56M16.45 16.45l1.56 1.56M3.5 12h2.2M18.3 12h2.2M5.99 18.01l1.56-1.56M16.45 7.55l1.56-1.56M12 16.2A4.2 4.2 0 1 0 12 7.8a4.2 4.2 0 0 0 0 8.4Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"/></svg>',
        dark: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M19.5 14.14A7.5 7.5 0 0 1 9.86 4.5a8.5 8.5 0 1 0 9.64 9.64Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"/></svg>'
    };

    function syncThemeControls(theme) {
        document.querySelectorAll('[data-theme-label]').forEach((node) => {
            node.textContent = theme === 'dark' ? '切换到浅色模式' : '切换到深色模式';
        });
        document.querySelectorAll('[data-theme-icon]').forEach((node) => {
            node.innerHTML = theme === 'dark' ? THEME_ICONS.light : THEME_ICONS.dark;
        });
        document.querySelectorAll('[data-theme-toggle]').forEach((button) => {
            const nextThemeLabel = theme === 'dark' ? '切换到浅色模式' : '切换到深色模式';
            button.setAttribute('aria-label', nextThemeLabel);
            button.setAttribute('title', nextThemeLabel);
        });
    }

    function resolvePreferredTheme() {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored === 'light' || stored === 'dark') {
            return stored;
        }
        return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }

    function applyTheme(theme) {
        root.setAttribute('data-theme', theme);
        localStorage.setItem(STORAGE_KEY, theme);
        syncThemeControls(theme);
    }

    function toggleTheme() {
        const nextTheme = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
        applyTheme(nextTheme);
    }

    function bindThemeToggles() {
        document.querySelectorAll('[data-theme-toggle]').forEach((button) => {
            if (button.dataset.themeBound === 'true') {
                return;
            }
            button.dataset.themeBound = 'true';
            button.addEventListener('click', toggleTheme);
        });
    }

    const mobileCollapsibleSyncQueue = new Set();

    function resolveMobileCollapsibleQuery(container) {
        const breakpoint = container.dataset.mobileBreakpoint || '768px';
        if (!container._mobileCollapsibleQuery) {
            container._mobileCollapsibleQuery = window.matchMedia(`(max-width: ${breakpoint})`);
        }
        return container._mobileCollapsibleQuery;
    }

    function updateMobileCollapsible(container) {
        const button = container.querySelector('[data-mobile-collapse-button]');
        const content = container.querySelector('[data-mobile-collapse-content]');
        if (!button || !content) {
            return;
        }
        const isMobile = resolveMobileCollapsibleQuery(container).matches;
        const isExpanded = isMobile ? container.dataset.mobileExpanded !== 'false' : true;
        container.classList.toggle('mobile-collapsible-active', isMobile);
        container.classList.toggle('mobile-collapsible-open', isExpanded);
        button.setAttribute('aria-expanded', isExpanded ? 'true' : 'false');
        content.hidden = isMobile ? !isExpanded : false;
    }

    function registerMobileCollapsible(container) {
        const button = container.querySelector('[data-mobile-collapse-button]');
        const content = container.querySelector('[data-mobile-collapse-content]');
        if (!button || !content) {
            return;
        }
        if (container.dataset.mobileCollapsibleBound !== 'true') {
            container.dataset.mobileCollapsibleBound = 'true';
            if (container.dataset.mobileExpanded !== 'true' && container.dataset.mobileExpanded !== 'false') {
                container.dataset.mobileExpanded = container.dataset.mobileOpen === 'true' ? 'true' : 'false';
            }
            button.addEventListener('click', () => {
                if (!resolveMobileCollapsibleQuery(container).matches) {
                    return;
                }
                container.dataset.mobileExpanded = container.dataset.mobileExpanded === 'false' ? 'true' : 'false';
                updateMobileCollapsible(container);
            });
            const query = resolveMobileCollapsibleQuery(container);
            const sync = () => updateMobileCollapsible(container);
            mobileCollapsibleSyncQueue.add(sync);
            if (typeof query.addEventListener === 'function') {
                query.addEventListener('change', sync);
            } else if (typeof query.addListener === 'function') {
                query.addListener(sync);
            }
        }
        updateMobileCollapsible(container);
    }

    function syncMobileCollapsibles(scope = document) {
        const containers = [];
        if (scope instanceof Element && scope.matches('[data-mobile-collapsible]')) {
            containers.push(scope);
        }
        if (scope.querySelectorAll) {
            scope.querySelectorAll('[data-mobile-collapsible]').forEach((container) => {
                containers.push(container);
            });
        }
        containers.forEach(registerMobileCollapsible);
    }

    function setMessage(element, text, type) {
        if (!element) {
            return;
        }
        element.textContent = text || '';
        element.className = 'status-message' + (type ? ' ' + type : '');
    }

    function formatTime(value) {
        if (!value) {
            return '-';
        }
        return String(value).replace('T', ' ');
    }

    function formatSize(size) {
        if (size === null || size === undefined || size === '') {
            return '-';
        }
        const units = ['B', 'KB', 'MB', 'GB', 'TB'];
        let value = Number(size);
        let index = 0;
        while (value >= 1024 && index < units.length - 1) {
            value /= 1024;
            index += 1;
        }
        return `${value.toFixed(value >= 10 || index === 0 ? 0 : 2)} ${units[index]}`;
    }

    async function requestJson(url, options = {}) {
        const response = await fetch(url, options);
        return response.json();
    }

    async function copyText(text) {
        await navigator.clipboard.writeText(text);
    }

    function buildShareUrl(link) {
        if (!link) {
            return '#';
        }
        if (link.sharePath) {
            return window.location.origin + link.sharePath;
        }
        if (link.linkId) {
            return window.location.origin + '/link/' + link.linkId;
        }
        return '#';
    }

    window.KiCloudApp = {
        applyTheme,
        bindThemeToggles,
        buildShareUrl,
        copyText,
        formatSize,
        formatTime,
        syncMobileCollapsibles,
        requestJson,
        resolvePreferredTheme,
        setMessage,
        syncThemeControls,
        toggleTheme
    };

    root.setAttribute('data-theme', resolvePreferredTheme());

    document.addEventListener('DOMContentLoaded', () => {
        applyTheme(root.getAttribute('data-theme') || resolvePreferredTheme());
        bindThemeToggles();
        syncMobileCollapsibles();
    });

    window.addEventListener('orientationchange', () => {
        mobileCollapsibleSyncQueue.forEach((sync) => sync());
    });
})();
