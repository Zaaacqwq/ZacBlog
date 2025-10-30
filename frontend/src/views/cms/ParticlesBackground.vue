<!-- particles background hidden -->
<template>
    <div id="particles-js"></div>
</template>

<script>
import "particles.js";

export default {
    mounted() {
        particlesJS("particles-js", {
            particles: {
                number: { value: 100 },
                color: { value: "#000000" },
                shape: { type: "circle" },
                opacity: { value: 0.5 },
                size: { value: 4, random: true,},
                line_linked: { enable: true, distance: 150, color: "#000000", opacity: 0.4, width: 1 },
                move: { enable: true, speed: 2, direction: "none", out_mode: "out" },
            },
            interactivity: {
                detect_on: "canvas",
                events: {
                    onhover: { enable: true, mode: "repulse" },
                    onclick: { enable: true, mode: "push" },
                },
                modes: {
                    repulse: { distance: 100 },
                    push: { particles_nb: 4 },
                },
            },
            retina_detect: true,
        });
    },
};
</script>

<style scoped>
#particles-js {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: -1;
    /* Ensure it's behind other content */
    pointer-events: none;
    /* Prevent interaction with the particles */
}
</style>