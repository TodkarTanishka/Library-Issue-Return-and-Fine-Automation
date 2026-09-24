const LibraryAPI = {
    async request(path, params = {}) {
        const q = new URLSearchParams(params);
        const res = await fetch(`/api/${path}${q.toString() ? '?' + q.toString() : ''}`);
        return await res.json();
    },
    async login(userId, password, role = '', loginType = 'user') {
        return this.request('auth/login', { userId, password, role, loginType });
    },
    async getBooks(search = '', department = 'all') {
        const r = await this.request('books', { search, department });
        return r.books || [];
    },
    async getLoans(userId = '') {
        const r = await this.request('loans', { userId });
        return r.loans || [];
    },
    async getStats() {
        const r = await this.request('stats');
        return r.stats || {};
    },
    async getWaitlist(bookId = '') {
        const r = await this.request('waitlists', { bookId });
        return r.waitlists || [];
    },
    async directIssueBook(userId, bookId) {
        return { success: false, message: "Direct issue disabled. All issues require librarian approval." };
    },
    async issueBook(userId, bookId) {
        return this.request('issue-request', { userId, bookId });
    },
    async issueBooks(userId, bookIds) {
        return this.request('issue-requests', { userId, bookIds: bookIds.join(',') });
    },
    async getMyRequests(userId) {
        const r = await this.request('my-requests', { userId });
        return r.requests || [];
    },
    async getPendingRequests(librarianId = '') {
        const r = await this.request('pending-requests', { librarianId });
        return r.requests || [];
    },
    async approveRequest(requestId, librarianId) {
        return this.request('approve-request', { requestId, librarianId });
    },
    async rejectRequest(requestId, librarianId, reason='Rejected by librarian.') {
        return this.request('reject-request', { requestId, librarianId, reason });
    },
    async returnBook(loanId, userId = '') {
        return this.request('return', { loanId, userId });
    },
    async approveReturnBook(loanId, librarianId = '') {
        return this.request('approve-return', { loanId, librarianId });
    },
    async rejectReturnBook(loanId, librarianId = '', reason = 'Rejected by librarian.') {
        return this.request('reject-return', { loanId, librarianId, reason });
    },
    async getPendingReturns(librarianId = '') {
        const r = await this.request('pending-returns', { librarianId });
        return r.returns || [];
    },
    async getUsers(adminId = '') {
        const r = await this.request('users', { adminId });
        return r.users || [];
    },
    async getFailureSimulation(librarianId='') {
        return this.request('failure-sim', { librarianId });
    },
    async addBook(role, librarianId, book) {
        return this.request('add-book', {
            role, librarianId,
            isbn: book.isbn, title: book.title, author: book.author,
            category: book.category || 'General',
            department: book.department || 'Computer Engineering',
            copies: book.copies || book.total_copies || 1
        });
    },
    async updateBook(role, librarianId, book) {
        return this.request('update-book', {
            role, librarianId,
            bookId: book.book_id || book.id,
            title: book.title, author: book.author, isbn: book.isbn,
            category: book.category || 'General',
            department: book.department || 'Computer Engineering',
            copies: book.copies || book.total_copies || 1
        });
    },
    async deleteBook(role, librarianId, bookId) {
        return this.request('delete-book', { role, librarianId, bookId });
    },
    async joinWaitlist(userId, bookId) {
        return this.request('join-waitlist', { userId, bookId });
    },
    async leaveWaitlist(waitlistId, userId) {
        return this.request('leave-waitlist', { waitlistId, userId });
    },
    async getMyWaitlist(userId) {
        const r = await this.request('my-waitlist', { userId });
        return r.waitlists || [];
    },
    async getNotifications(userId, role = 'user') {
        const r = await this.request('notifications', { userId, role });
        return r.notifications || [];
    },
    async getUnreadNotificationsCount(userId, role = 'user') {
        const r = await this.request('notifications/unread-count', { userId, role });
        return r.unreadCount || 0;
    },
    async markNotificationRead(id, userId) {
        return this.request('notifications/read', { id, userId });
    },
    async markAllNotificationsRead(userId, role = 'user') {
        return this.request('notifications/read-all', { userId, role });
    },
    async getMyFines(userId) {
        const r = await this.request('my-fines', { userId });
        return r.fines || [];
    },
    async getAllFines(librarianId = '') {
        const r = await this.request('fines', { librarianId });
        return r.fines || [];
    },
    async settleFine(fineId, amountPaid, paymentMethod = 'CASH', paymentRef = '', librarianId = '') {
        return this.request('settle-fine', { fineId, amountPaid, paymentMethod, paymentRef, librarianId });
    },
    async suggestBooks(q = '', limit = 8) {
        return this.request('books/suggest', { q, limit });
    },
    async getUrgentOverdue(limit = 10) {
        const r = await this.request('overdue/urgent', { limit });
        return r.urgent || [];
    },
    async timeTravel(days = 0) {
        return this.request('admin/time-travel', { days });
    },
    async getDsaState() {
        return this.request('dsa/state');
    },
    async runBenchmark() {
        return this.request('admin/benchmark');
    },
    async getKpiHistory() {
        const r = await this.request('admin/kpi-history');
        return r.history || [];
    },
    async generateReceipt(loanId) {
        return this.request('receipt', { loanId });
    }
};
window.LibraryAPI = LibraryAPI;
