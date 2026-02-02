/**
 * @file Terminal.tsx
 * @description The main interface component. Renders the scrolling message log
 * and the input area with its associated floating command menu.
 */

import React, { useEffect, useRef, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { MessageType, TerminalMessage, SystemContext } from '../types';
import CommandMenu from './CommandMenu';
import { getCommandSuggestions } from '../constants';

interface TerminalProps {
  messages: TerminalMessage[];      // List of log messages to display
  onCommand: (cmd: string) => void; // Callback when user hits Enter
  input: string;                    // Controlled input state
  setInput: (val: string) => void;  // State setter
  context: SystemContext;           // Current OS context for autocomplete
}

const Terminal: React.FC<TerminalProps> = ({ messages, onCommand, input, setInput, context }) => {
  const bottomRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  
  // Tracks position in command history. -1 indicates "new input" mode.
  const [historyIndex, setHistoryIndex] = useState(-1);

  // Derive history array from command messages to allow navigation (Up/Down arrow)
  const history = messages
    .filter(msg => msg.type === MessageType.COMMAND)
    .map(msg => msg.content);

  // Auto-scroll to the bottom whenever a new message arrives
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Aggressive Auto-focus: Ensures the terminal input always captures keystrokes
  // even if the user clicks away.
  useEffect(() => {
    const focusInterval = setInterval(() => {
        if (document.activeElement !== inputRef.current) {
            inputRef.current?.focus();
        }
    }, 1000);
    return () => clearInterval(focusInterval);
  }, []);

  /**
   * Handles the execution of a command.
   */
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim()) return;
    onCommand(input);
    setInput('');
    setHistoryIndex(-1); // Reset history pointer to the "bottom"
  };

  /**
   * Handles selecting an item from the autocomplete menu.
   * Smartly appends or replaces text based on cursor position and word boundaries.
   */
  const handleMenuSelect = (valueToAppend: string) => {
    // If valueToAppend is empty (just a group label), do nothing
    if (!valueToAppend) return;

    let newVal = input;
    const parts = input.trimEnd().split(' ');
    const lastPart = parts[parts.length - 1] || '';

    // If the input ends with the start of the selected value, replace it (autocomplete)
    if (valueToAppend.startsWith(lastPart) && lastPart.length > 0) {
         newVal = input.substring(0, input.length - lastPart.length) + valueToAppend;
    } else {
        // Otherwise append as a new argument
        newVal = input + (input.endsWith(' ') || input.length === 0 ? '' : ' ') + valueToAppend;
    }
    
    setInput(newVal + ' '); // Add trailing space for the next argument
    inputRef.current?.focus();
  };

  /**
   * Key handler for Tab completion and History navigation.
   */
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Tab') {
      e.preventDefault();
      // Calculate valid suggestions for the current input
      const suggestions = getCommandSuggestions(input, context);
      
      // If there is exactly one match, auto-fill it
      if (suggestions.length === 1) {
        handleMenuSelect(suggestions[0].value);
      }
    } else if (e.key === 'ArrowUp') {
      // Navigate Backwards in History
      e.preventDefault();
      if (history.length === 0) return;

      const newIndex = historyIndex === -1 ? history.length - 1 : Math.max(0, historyIndex - 1);
      setHistoryIndex(newIndex);
      setInput(history[newIndex]);
    } else if (e.key === 'ArrowDown') {
      // Navigate Forwards in History
      e.preventDefault();
      if (historyIndex === -1) return; // Already at new input

      if (historyIndex < history.length - 1) {
        const newIndex = historyIndex + 1;
        setHistoryIndex(newIndex);
        setInput(history[newIndex]);
      } else {
        // Reached the end, clear input
        setHistoryIndex(-1);
        setInput('');
      }
    }
  };

  return (
    <div className="h-full flex flex-col font-mono text-guide-green p-4 overflow-hidden relative">
      {/* Visual Effect: Scanline Overlay using linear gradients */}
      <div className="absolute inset-0 pointer-events-none bg-[linear-gradient(rgba(18,16,16,0)_50%,rgba(0,0,0,0.25)_50%),linear-gradient(90deg,rgba(255,0,0,0.06),rgba(0,255,0,0.02),rgba(0,0,255,0.06))] z-10 bg-[length:100%_2px,3px_100%] opacity-20"></div>

      {/* Output Log Area */}
      <div className="flex-1 overflow-y-auto pr-2 custom-scrollbar space-y-1 mb-20 relative z-0">
        <AnimatePresence initial={false}>
          {messages.map((msg) => (
            <motion.div
              key={msg.id}
              initial={{ opacity: 0, x: -10 }} // Slide-in animation
              animate={{ opacity: 1, x: 0 }}
              className={`
                break-words text-sm md:text-base leading-tight
                ${msg.type === MessageType.COMMAND ? 'text-guide-cyan font-bold mt-2' : ''}
                ${msg.type === MessageType.ERROR ? 'text-guide-darkRed' : ''}
                ${msg.type === MessageType.SYSTEM ? 'text-guide-orange' : ''}
              `}
            >
              <span className="opacity-50 mr-2 select-none text-xs">
                {msg.type === MessageType.COMMAND ? '>' : ''}
              </span>
              {msg.content}
            </motion.div>
          ))}
        </AnimatePresence>
        <div ref={bottomRef} />
      </div>

      {/* Input Handling Section */}
      <div className="absolute bottom-0 left-0 w-full px-4 pb-4 pt-2 bg-gradient-to-t from-[#111] to-transparent z-20">
        
        {/* Dynamic Command Menu - Floats physically above input */}
        <div className="mb-2">
             <CommandMenu input={input} onSelect={handleMenuSelect} context={context} />
        </div>

        {/* Input Field Form */}
        <form onSubmit={handleSubmit} className="flex items-center relative bg-[#222] rounded-md border border-[#333] px-3 py-2 shadow-lg">
          <span className="text-guide-cyan mr-2 animate-pulse font-bold">{'>'}</span>
          <input
            ref={inputRef}
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            autoFocus
            className="flex-1 bg-transparent border-none outline-none text-guide-green placeholder-guide-green/30 font-bold font-mono"
            spellCheck={false}
            autoComplete="off"
          />
        </form>
      </div>
    </div>
  );
};

export default Terminal;