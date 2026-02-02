import { GoogleGenAI, Type } from "@google/genai";
import { GuideData } from "../types";

const apiKey = process.env.API_KEY || '';

// Initialize safely, even if key is missing (handled in UI)
const ai = apiKey ? new GoogleGenAI({ apiKey }) : null;

export const fetchGuideEntry = async (query: string): Promise<GuideData> => {
  if (!ai) {
    throw new Error("API Key missing");
  }

  try {
    const systemPrompt = `
    You are a visual manual page generator for a terminal interface.
    The user will provide a terminal command (e.g., "git commit", "grep", "ls").
    
    Your goal is to provide a concise, structured explanation of what the command does.
    
    Tone: Professional, concise, slightly technical but accessible.
    
    Return a JSON object with the following schema:
    - title: string (The command name, e.g., "GIT COMMIT")
    - description: string (A concise explanation of the command's function and typical usage. Max 40 words.)
    - dangerLevel: string (Assess the risk of running this command: 'Low' (safe), 'Medium' (modifies data), 'High' (deletes data), 'Extreme' (system wide changes))
    - visualType: string (Choose an icon metaphor: 'robot' (automation), 'fish' (translation/network), 'ship' (deployment/movement), 'planet' (global/environment), 'default' (generic))
    `;

    const response = await ai.models.generateContent({
      model: "gemini-3-flash-preview",
      contents: `Explain command: ${query}`,
      config: {
        systemInstruction: systemPrompt,
        responseMimeType: "application/json",
        responseSchema: {
          type: Type.OBJECT,
          properties: {
            title: { type: Type.STRING },
            description: { type: Type.STRING },
            dangerLevel: { type: Type.STRING, enum: ['Low', 'Medium', 'High', 'Extreme', 'Mostly Harmless'] },
            visualType: { type: Type.STRING, enum: ['planet', 'robot', 'fish', 'ship', 'default'] }
          },
          required: ["title", "description", "dangerLevel", "visualType"]
        }
      }
    });

    const text = response.text;
    if (!text) throw new Error("No response from Manual");

    return JSON.parse(text) as GuideData;

  } catch (error) {
    console.error("Manual retrieval failed:", error);
    return {
      title: "MANUAL ERROR",
      description: "Unable to retrieve documentation for the specified command. Please check your network connection.",
      dangerLevel: "Low",
      visualType: "robot"
    };
  }
};