const Common = {
    
    async fetchJson(url, options = {}) {
        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json'
            }
        };
        
        const response = await fetch(url, { ...defaultOptions, ...options });
        return response.json();
    },

    showAlert(message, type = 'info') {
        alert(message);
    },

    confirm(message) {
        return window.confirm(message);
    },

    formatDate(dateString) {
        if (!dateString) return '';
        const date = new Date(dateString);
        return date.toLocaleDateString('ko-KR');
    }
};
