<template>
  <div id="app">
    <nav>
      <RouterLink to="/" class="brand">JpopRadar</RouterLink>
      <div class="nav-links">
        <RouterLink to="/">Home</RouterLink>
<RouterLink to="/concerts">Concerts</RouterLink>
      </div>
    </nav>
    <main>
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>
