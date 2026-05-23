document.addEventListener('DOMContentLoaded', () => {
    // Scroll Reveal Animation
    const revealElements = document.querySelectorAll('.reveal');

    const revealOnScroll = () => {
        const windowHeight = window.innerHeight;
        const elementVisible = 100;

        revealElements.forEach(element => {
            const elementTop = element.getBoundingClientRect().top;
            if (elementTop < windowHeight - elementVisible) {
                element.classList.add('active');
            }
        });
    };

    // Initial check in case elements are already in view
    revealOnScroll();

    // Listen for scroll events
    window.addEventListener('scroll', revealOnScroll);

    // Mouse parallax effect for background gradients
    const bgRed = document.querySelector('.bg-red');
    const bgBlue = document.querySelector('.bg-blue');

    document.addEventListener('mousemove', (e) => {
        const x = e.clientX / window.innerWidth;
        const y = e.clientY / window.innerHeight;

        bgRed.style.transform = `translate(${x * 20}px, ${y * 20}px)`;
        bgBlue.style.transform = `translate(${x * -30}px, ${y * -30}px)`;
    });
});
