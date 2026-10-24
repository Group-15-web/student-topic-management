document.addEventListener('DOMContentLoaded', () => {
  document
    .querySelector('.menu-toggle')
    ?.addEventListener('click', () => document.body.classList.toggle('nav-open'));
  document.querySelectorAll('form[data-confirm]').forEach((form) =>
    form.addEventListener('submit', (event) => {
      if (!confirm(form.dataset.confirm)) event.preventDefault();
    }),
  );
});
