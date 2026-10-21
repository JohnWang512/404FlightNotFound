import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

// HomeView is eagerly imported because it's the landing page — all other routes
// use dynamic imports so their bundles are only fetched when first visited.
const routes = [
    {
        path: '/',
        name: 'Home',
        component: HomeView
    },
    {
        path: '/flight_api',
        name: 'FlightApiTest',
        component: () => import('../views/FlightApiTest.vue')
    },
    {
        path: '/api-explorer',
        name: 'ApiExplorer',
        component: () => import('../views/ApiExplorer.vue')
    },
    {
        path: '/db-explorer',
        name: 'DbExplorer',
        component: () => import('../views/DbExplorer.vue')
    },
    {
        path: '/watchlist',
        name: 'Watchlist',
        component: () => import('../views/WatchlistView.vue')
    },
    // Catch-all: unknown paths redirect to home rather than showing a 404
    {
        path: '/:pathMatch(.*)*',
        redirect: '/'
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
