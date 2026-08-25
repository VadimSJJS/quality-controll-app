/*
 * Переключатель видимости пароля («глазик»).
 * Поддерживает пары полей:
 *   - страница входа:   #password       + #togglePassword
 *   - шапка приложения: #headerPassword + #toggleHeaderPassword
 */
(function () {
    function initToggle(inputId, buttonId) {
        var pwd = document.getElementById(inputId);
        var btn = document.getElementById(buttonId);
        if (!pwd || !btn || btn.dataset.passwordToggleBound) {
            return;
        }
        btn.dataset.passwordToggleBound = 'true';
        var icon = btn.querySelector('i');
        btn.addEventListener('click', function () {
            var type = pwd.getAttribute('type') === 'password' ? 'text' : 'password';
            pwd.setAttribute('type', type);
            if (icon) {
                icon.classList.toggle('bi-eye-slash');
                icon.classList.toggle('bi-eye');
            }
            btn.setAttribute('aria-label', type === 'password' ? 'Показать пароль' : 'Скрыть пароль');
            // На странице входа возвращаем фокус в поле пароля
            if (inputId === 'password') {
                pwd.focus();
            }
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        initToggle('password', 'togglePassword');
        initToggle('headerPassword', 'toggleHeaderPassword');
    });
})();
