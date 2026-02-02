export enum MessageType {
  COMMAND = 'COMMAND',
  OUTPUT = 'OUTPUT',
  ERROR = 'ERROR',
  SYSTEM = 'SYSTEM'
}

export interface TerminalMessage {
  id: string;
  type: MessageType;
  content: string;
  timestamp: number;
}

export interface MenuOption {
  id: string;
  label: string;
  value: string; // The text to append to the command line
  color: string; // Tailwind class
  children?: MenuOption[];
}

export enum ViewState {
  BOOTING = 'BOOTING',
  ACTIVE = 'ACTIVE',
  PANIC = 'PANIC'
}

export interface GuideData {
  title: string;
  description: string;
  dangerLevel: string;
  visualType: string;
}

export type OSType = 'ubuntu' | 'macos' | 'windows';

export interface SystemContext {
  os: OSType;
  user: string;
  hostname: string;
}