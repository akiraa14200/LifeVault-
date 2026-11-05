import type { Config } from "tailwindcss";
const config: Config = {
  content: ["./src/**/*.{js,ts,jsx,tsx,mdx}"],
  theme: { extend: {
    fontFamily: { sans: ["var(--font-geist)", "Inter", "sans-serif"] },
    colors: { ink: "#17211d", mint: "#b8e5d1", forest: "#234c3d", canvas: "#f4f6f2" },
    boxShadow: { glass: "0 18px 60px rgba(28,55,43,.08)" }
  }},
  plugins: []
};
export default config;