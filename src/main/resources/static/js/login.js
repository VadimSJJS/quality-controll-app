/**
 * Обработка блокировки учётной записи после неудачных попыток входа.
 * Проверяет URL-параметры ?blocked=true&minutes=N и отображает
 * сообщение о блокировке с обратным отсчётом.
 */
document.addEventListener('DOMContentLoaded', function () {

    const urlParams = new URLSearchParams(window.location.search);
    const isBlocked = urlParams.get('blocked') === 'true';
    const minutesParam = parseInt(urlParams.get('minutes'), 10);

    if (!isBlocked || isNaN(minutesParam) || minutesParam <= 0) {
        return;
    }

    // Показываем блок о блокировке
    const blockedAlert = document.getElementById('blockedAlert');
    const countdownEl = document.getElementById('countdown');
    const loginForm = document.getElementById('loginForm');
    const loginBtn = document.getElementById('loginBtn');

    if (blockedAlert) {
        blockedAlert.style.display = 'flex';
    }

    // Блокируем форму
    if (loginForm) {
        loginForm.classList.add('form-locked');
    }
    if (loginBtn) {
        loginBtn.disabled = true;
        loginBtn.innerHTML = '<span>Ожидание...</span><i class="bi bi-clock"></i>';
    }

    // Обратный отсчёт
    let remainingSeconds = minutesParam * 60;

    function updateCountdown() {
        const mins = Math.floor(remainingSeconds / 60);
        const secs = remainingSeconds % 60;
        const text = `Попробуйте войти через ${mins}:${secs.toString().padStart(2, '0')}`;

        if (countdownEl) {
            countdownEl.textContent = text;
        }

        if (remainingSeconds <= 0) {
            // Время вышло — разблокируем форму и перезагружаем страницу
            if (loginForm) {
                loginForm.classList.remove('form-locked');
            }
            if (loginBtn) {
                loginBtn.disabled = false;
                loginBtn.innerHTML = '<span>Войти в систему</span><i class="bi bi-arrow-right"></i>';
            }
            if (blockedAlert) {
                blockedAlert.style.display = 'none';
            }
            // Убираем параметры из URL без перезагрузки
            const url = new URL(window.location);
            url.searchParams.delete('blocked');
            url.searchParams.delete('minutes');
            window.history.replaceState({}, '', url);
            return;
        }

        remainingSeconds--;
    }

    updateCountdown();
    setInterval(updateCountdown, 1000);
});
