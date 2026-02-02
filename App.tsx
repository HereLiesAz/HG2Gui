/**
 * @file App.tsx
 * @description The main application controller.
 * Manages the global state (SystemContext, Message Log) and layout.
 * It handles the top-level window chrome (or lack thereof) and command routing.
 */

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import Terminal from './components/Terminal';
import { TerminalMessage, MessageType, ViewState, SystemContext, OSType } from './types';
import { INITIAL_WELCOME_MSG } from './constants';

const App: React.FC = () => {
  // --- State Management ---
  const [messages, setMessages] = useState<TerminalMessage[]>([]); // Command history log
  const [currentInput, setCurrentInput] = useState('');            // Controlled input for Terminal
  const [viewState, setViewState] = useState<ViewState>(ViewState.BOOTING); // Overall screen state
  
  // System Context: Simulates the underlying OS environment
  // Changing this changes which commands are available in the menu.
  const [context, setContext] = useState<SystemContext>({
    os: 'ubuntu',
    user: 'root',
    hostname: 'hitchhiker-guide'
  });

  // --- Effects ---

  // 1. Initial Boot Sequence
  // Simulates a hardware startup delay before showing the interactive terminal.
  useEffect(() => {
    const boot = async () => {
      await new Promise(r => setTimeout(r, 1000));
      setViewState(ViewState.ACTIVE);
      addMessage(MessageType.SYSTEM, INITIAL_WELCOME_MSG);
    };
    boot();
  }, []);

  // 2. Panic Monitor
  // Easter Egg: If an ERROR message is received, trigger the "Don't Panic" overlay
  // for a brief comedic effect.
  useEffect(() => {
    if (messages.length === 0) return;
    const lastMsg = messages[messages.length - 1];
    
    if (lastMsg.type === MessageType.ERROR && viewState !== ViewState.PANIC) {
        setViewState(ViewState.PANIC);
        setTimeout(() => {
            setViewState(ViewState.ACTIVE);
        }, 3000);
    }
  }, [messages]);

  /**
   * Helper to append a new message to the log.
   * Generates a unique ID and timestamp.
   */
  const addMessage = (type: MessageType, content: string) => {
    setMessages(prev => [...prev, {
      id: Math.random().toString(36).substr(2, 9),
      type,
      content,
      timestamp: Date.now()
    }]);
  };

  /**
   * Main Command Processor.
   * Intercepts specific commands (like 'clear' or 'switch-os') for client-side logic,
   * otherwise simulates a generic execution response.
   */
  const handleCommand = async (cmd: string) => {
    addMessage(MessageType.COMMAND, cmd);
    const lowerCmd = cmd.toLowerCase().trim();
    const parts = lowerCmd.split(' ');

    // 1. Internal Command: CLEAR
    if (lowerCmd === 'clear') {
      setMessages([]);
      return;
    }

    // 2. Internal Command: SWITCH-OS
    // Updates the SystemContext to demonstrate dynamic menus.
    if (parts[0] === 'switch-os' && parts[1]) {
        const newOs = parts[1] as OSType;
        if (['ubuntu', 'macos', 'windows'].includes(newOs)) {
            setContext(prev => ({ ...prev, os: newOs }));
            addMessage(MessageType.SYSTEM, `>> SYSTEM REBOOT INITIALIZED...`);
            addMessage(MessageType.SYSTEM, `>> KERNEL SWITCHED TO: ${newOs.toUpperCase()}`);
            addMessage(MessageType.OUTPUT, `Menu options updated for ${newOs}.`);
        } else {
            addMessage(MessageType.ERROR, `OS '${parts[1]}' NOT FOUND.`);
        }
        return;
    }
    
    // 3. Simulated Responses for other commands
    setTimeout(() => {
        if (lowerCmd.startsWith('sudo')) {
             if (context.os === 'windows') {
                 addMessage(MessageType.ERROR, "'sudo' is not recognized as an internal or external command.");
             } else {
                 addMessage(MessageType.OUTPUT, `[sudo] password for ${context.user}: **********`);
                 setTimeout(() => addMessage(MessageType.OUTPUT, "Operation successful."), 500);
             }
        } else if (lowerCmd.startsWith('brew')) {
             if (context.os !== 'macos') {
                 addMessage(MessageType.ERROR, "brew: command not found");
             } else {
                 addMessage(MessageType.OUTPUT, "🍺 Updating Homebrew...");
             }
        } else if (lowerCmd.startsWith('git')) {
             addMessage(MessageType.OUTPUT, "git: " + cmd + " executed on branch main.");
        } else if (lowerCmd.startsWith('docker')) {
             addMessage(MessageType.OUTPUT, "CONTAINER ID   IMAGE     COMMAND   CREATED   STATUS");
        } else {
             addMessage(MessageType.OUTPUT, `Executed: ${cmd}`);
        }
    }, 200);
  };

  return (
    <div className="w-screen h-screen bg-[#111] flex flex-col overflow-hidden relative text-guide-green font-mono">
      {/* Top Bar: Displays current system metadata (OS/User) */}
      <div className="h-8 w-full flex items-center px-4 justify-end bg-[#0a0a0a] border-b border-[#222] select-none z-20">
         <div className="flex gap-4 text-xs font-mono text-guide-offWhite opacity-30 tracking-widest uppercase">
            <span>OS: {context.os}</span>
            <span>USER: {context.user}</span>
         </div>
      </div>

      <div className="flex-1 relative overflow-hidden">
         {/* Panic Overlay: Only visible during ERROR states */}
         {viewState === ViewState.PANIC && (
            <motion.div 
                initial={{ opacity: 0, scale: 0.5 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0 }}
                className="absolute inset-0 flex items-center justify-center bg-guide-green z-50 pointer-events-none"
            >
                <motion.h1 
                    animate={{ scale: [1, 1.2, 1], rotate: [0, -5, 5, 0] }}
                    transition={{ repeat: Infinity, duration: 0.5 }}
                    className="text-6xl md:text-8xl font-black text-guide-darkRed text-center font-rounded tracking-tighter"
                >
                    DON'T<br/>PANIC
                </motion.h1>
            </motion.div>
         )}

        {/* Main Terminal Component */}
        <Terminal 
          messages={messages} 
          onCommand={handleCommand}
          input={currentInput}
          setInput={setCurrentInput}
          context={context}
        />
      </div>
    </div>
  );
};

export default App;