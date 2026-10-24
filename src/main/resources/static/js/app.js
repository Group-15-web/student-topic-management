document.addEventListener('DOMContentLoaded', () => {
  document
    .querySelector('.menu-toggle')
    ?.addEventListener('click', () => document.body.classList.toggle('nav-open'));
  document.querySelectorAll('form[data-confirm]').forEach((form) =>
    form.addEventListener('submit', (event) => {
      if (!confirm(form.dataset.confirm)) event.preventDefault();
    }),
  );
  document.querySelectorAll('[data-demo]').forEach((button) =>
    button.addEventListener('click', () => {
      document.getElementById('login-email').value = button.dataset.demo;
      document.getElementById('login-password').value = 'Demo@2026!';
      document.getElementById('login-email').focus();
    }),
  );
});
