import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  experimental: {
    optimizePackageImports: ['@heroicons/react', '@livekit/components-react'],
  },
};

export default nextConfig;
