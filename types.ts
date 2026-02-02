/**
 * @file types.ts
 * @description Defines the core TypeScript interfaces and Enums used across the application.
 * Centralizing types here ensures consistency between components and logic.
 */

/**
 * Categorizes the nature of a message in the terminal.
 * Used for styling and filtering logic.
 */
export enum MessageType {
  COMMAND = 'COMMAND', // User input
  OUTPUT = 'OUTPUT',   // Standard stdout response
  ERROR = 'ERROR',     // Stderr or system failure
  SYSTEM = 'SYSTEM'    // Meta-messages from the emulator itself
}

/**
 * Represents a single line or block of text in the terminal history.
 */
export interface TerminalMessage {
  id: string;        // Unique identifier for React keys
  type: MessageType; // The category of message
  content: string;   // The actual text content
  timestamp: number; // Unix timestamp for ordering
}

/**
 * Represents a node in the command suggestion tree.
 * Used by the CommandMenu to render interactive options.
 */
export interface MenuOption {
  id: string;        // Unique ID
  label: string;     // Display text (e.g., "COMMIT")
  value: string;     // Value to append to input (e.g., "commit")
  color: string;     // Tailwind CSS color class for the button
  children?: MenuOption[]; // Nested sub-commands
}

/**
 * Defines the macro state of the application screen.
 */
export enum ViewState {
  BOOTING = 'BOOTING', // Initial startup animation
  ACTIVE = 'ACTIVE',   // Standard interactive mode
  PANIC = 'PANIC'      // "Don't Panic" overlay state
}

/**
 * Structure expected from the AI (Gemini) when generating manual pages.
 */
export interface GuideData {
  title: string;       // Command Name
  description: string; // Short summary
  dangerLevel: string; // Risk assessment (Low/Medium/High/Extreme)
  visualType: string;  // Icon mapping key (robot/fish/ship/etc)
}

/**
 * Supported Operating System contexts.
 * This determines which package managers and paths are valid.
 */
export type OSType = 'ubuntu' | 'macos' | 'windows';

/**
 * The global context of the simulated machine.
 */
export interface SystemContext {
  os: OSType;      // Current OS simulation
  user: string;    // Current logged-in user
  hostname: string; // Machine name
}