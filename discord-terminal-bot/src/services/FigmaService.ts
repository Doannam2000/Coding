import https from 'https';

interface FigmaNode {
  id: string;
  name: string;
  type: string;
  children?: FigmaNode[];
  absoluteBoundingBox?: {
    x: number;
    y: number;
    width: number;
    height: number;
  };
  fills?: any[];
  strokes?: any[];
  style?: any;
  characters?: string;
  styleOverrideTable?: any;
}

interface FigmaFrame {
  id: string;
  name: string;
  width: number;
  height: number;
  children: FigmaNode[];
}

interface FigmaStyle {
  fill?: string;
  stroke?: string;
  strokeWidth?: number;
  fontFamily?: string;
  fontSize?: number;
  fontWeight?: number;
  textAlign?: string;
}

export interface FigmaDesignData {
  fileName: string;
  frames: FigmaFrame[];
  styles: FigmaStyle[];
  components: string[];
  colors: string[];
}

export class FigmaService {
  private apiKey: string;

  constructor() {
    this.apiKey = process.env.FIGMA_API_KEY || '';
  }

  setApiKey(key: string): void {
    this.apiKey = key;
  }

  extractFileKey(url: string): string | null {
    const patterns = [
      /figma\.com\/file\/([a-zA-Z0-9]+)/,
      /figma\.com\/design\/([a-zA-Z0-9]+)/,
      /figma\.com\/proto\/([a-zA-Z0-9]+)/,
    ];

    for (const pattern of patterns) {
      const match = url.match(pattern);
      if (match) return match[1];
    }
    return null;
  }

  async getFile(fileKey: string): Promise<any> {
    return new Promise((resolve, reject) => {
      const url = `figma.com/api/v1/files/${fileKey}`;
      const options: any = {
        hostname: 'www.figma.com',
        path: `/api/v1/files/${fileKey}`,
        method: 'GET',
        headers: {
          'X-Figma-Token': this.apiKey,
        },
      };

      if (this.apiKey) {
        options.headers['X-Figma-Token'] = this.apiKey;
      }

      const req = https.request(options, (res) => {
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => {
          try {
            const json = JSON.parse(data);
            resolve(json);
          } catch (e) {
            reject(new Error('Failed to parse Figma response'));
          }
        });
      });

      req.on('error', reject);
      req.end();
    });
  }

  extractColor(fills: any[]): string | null {
    if (!fills || fills.length === 0) return null;
    const fill = fills[0];
    if (fill.type === 'SOLID' && fill.color) {
      const { r, g, b } = fill.color;
      const a = fill.opacity ?? 1;
      if (a < 1) {
        return `rgba(${Math.round(r*255)}, ${Math.round(g*255)}, ${Math.round(b*255)}, ${a.toFixed(2)})`;
      }
      return `#${Math.round(r*255).toString(16).padStart(2,'0')}${Math.round(g*255).toString(16).padStart(2,'0')}${Math.round(b*255).toString(16).padStart(2,'0')}`;
    }
    return null;
  }

  extractTypography(style: any, characters?: string): string {
    const parts = [];
    if (style.fontFamily) parts.push(`font-family: ${style.fontFamily}`);
    if (style.fontSize) parts.push(`font-size: ${style.fontSize}px`);
    if (style.fontWeight) parts.push(`font-weight: ${style.fontWeight}`);
    if (style.textAlignHorizontal) parts.push(`text-align: ${style.textAlignHorizontal.toLowerCase()}`);
    return parts.join(', ') || characters || '';
  }

  async fetchDesign(url: string): Promise<FigmaDesignData> {
    const fileKey = this.extractFileKey(url);
    if (!fileKey) {
      throw new Error('Invalid Figma URL. Use format: figma.com/file/XXX');
    }

    const response = await this.getFile(fileKey);

    const colors = new Set<string>();
    const styles: FigmaStyle[] = [];
    const components: string[] = [];
    const frames: FigmaFrame[] = [];

    const processNode = (node: FigmaNode, depth = 0) => {
      if (node.fills) {
        const color = this.extractColor(node.fills);
        if (color) colors.add(color);
      }

      if (node.type === 'TEXT' && node.style) {
        styles.push({
          fontFamily: node.style.fontFamily,
          fontSize: node.style.fontSize,
          fontWeight: node.style.fontWeight,
          textAlign: node.style.textAlignHorizontal,
        });
      }

      if (node.type === 'COMPONENT' || node.type === 'COMPONENT_SET') {
        components.push(node.name);
      }

      if (node.children) {
        for (const child of node.children) {
          processNode(child, depth + 1);
        }
      }
    };

    if (response.document?.children) {
      for (const page of response.document.children) {
        if (page.children) {
          for (const frame of page.children) {
            if (frame.type === 'FRAME' || frame.type === 'COMPONENT') {
              const f: FigmaFrame = {
                id: frame.id,
                name: frame.name,
                width: frame.absoluteBoundingBox?.width || 0,
                height: frame.absoluteBoundingBox?.height || 0,
                children: [],
              };

              if (frame.children) {
                for (const child of frame.children) {
                  processNode(child);
                  f.children.push(child as any);
                }
              }

              frames.push(f);
            }
          }
        }
      }
    }

    return {
      fileName: response.name || 'Figma Design',
      frames,
      styles: styles.slice(0, 20),
      components,
      colors: Array.from(colors).slice(0, 20),
    };
  }

  generatePromptFromFigma(data: FigmaDesignData): string {
    let prompt = `# Figma Design: ${data.fileName}\n\n`;

    if (data.colors.length > 0) {
      prompt += `## Colors (use these in Tailwind)\n`;
      for (const color of data.colors) {
        prompt += `- ${color}\n`;
      }
      prompt += '\n';
    }

    if (data.components.length > 0) {
      prompt += `## Components Found\n`;
      for (const comp of data.components.slice(0, 15)) {
        prompt += `- ${comp}\n`;
      }
      prompt += '\n';
    }

    if (data.frames.length > 0) {
      prompt += `## Screens/Frames\n`;
      for (const frame of data.frames.slice(0, 10)) {
        prompt += `- ${frame.name} (${frame.width}x${frame.height})\n`;
      }
      prompt += '\n';
    }

    prompt += `## Task\nGenerate React + Tailwind CSS code for this design. Use the colors above, create semantic components matching the frame names. Include responsive design.`;

    return prompt;
  }
}

export const figmaService = new FigmaService();
