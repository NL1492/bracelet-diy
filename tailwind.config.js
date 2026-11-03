/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      colors: {
        cream: '#FBFAF6',
        'cream-dark': '#F0EDE8',
        'wood': '#8B6F4E',
        'wood-dark': '#6B4F3E',
        'gold': '#C8A96E',
      },
      fontFamily: {
        serif: ['"Noto Serif SC"', '"Source Han Serif SC"', 'STSong', 'serif'],
        sans: ['"PingFang SC"', '"Microsoft YaHei"', 'sans-serif'],
      },
      transitionDuration: {
        '400': '400ms',
        '600': '600ms',
      },
    },
  },
  plugins: [],
}
