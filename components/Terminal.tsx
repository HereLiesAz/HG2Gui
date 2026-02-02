import React, { useEffect, useRef, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { MessageType, TerminalMessage, SystemContext } from '../types';
import CommandMenu from './CommandMenu';
import { getCommandSuggestions } from '../constants';

interface TerminalProps {
  messages: TerminalMessage[];
  onCommand: (cmd: string) => void;
  input: string;
  setInput: (val: string) => void;
  context: SystemContext;
}

const Terminal: React.FC<TerminalProps> = ({ messages, onCommand, input, setInput, context }) => {
  const bottomRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const [historyIndex, setHistoryIndex] = useState(-1);

  // Derive history from messages to allow navigation
  const history = messages
    .filter(msg => msg.type === MessageType.COMMAND)
    .map(msg => msg.content);

  // Auto-scroll to bottom
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Keep focus on input
  useEffect(() => {
    const focusInterval = setInterval(() => {
        if (document.activeElement !== inputRef.current) {
            inputRef.current?.focus();
        }
    }, 1000);
    return () => clearInterval(focusInterval);
  }, []);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim()) return;
    onCommand(input);
    setInput('');
    setHistoryIndex(-1); // Reset history pointer
  };

  const handleMenuSelect = (valueToAppend: string) => {
    // If valueToAppend is empty (just a group label), do nothing
    if (!valueToAppend) return;

    let newVal = input;
    const parts = input.trimEnd().split(' ');
    const lastPart = parts[parts.length - 1] || '';

    // If matches the end of input, replace it
    if (valueToAppend.startsWith(lastPart) && lastPart.length > 0) {
         newVal = input.substring(0, input.length - lastPart.length) + valueToAppend;
    } else {
        // Otherwise append
        newVal = input + (input.endsWith(' ') || input.length === 0 ? '' : ' ') + valueToAppend;
    }
    
    setInput(newVal + ' '); // Add space for next command
    inputRef.current?.focus();
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Tab') {
      e.preventDefault();
      // Pass context to get correct suggestions for the current OS
      const suggestions = getCommandSuggestions(input, context);
      
      if (suggestions.length === 1) {
        // Unique match found, auto-complete
        handleMenuSelect(suggestions[0].value);
      }
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      if (history.length === 0) return;

      const newIndex = historyIndex === -1 ? history.length - 1 : Math.max(0, historyIndex - 1);
      setHistoryIndex(newIndex);
      setInput(history[newIndex]);
    } else if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (historyIndex === -1) return; // Already at the bottom (new input)

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
      {/* Scanline effect overlay */}
      <div className="absolute inset-0 pointer-events-none bg-[linear-gradient(rgba(18,16,16,0)_50%,rgba(0,0,0,0.25)_50%),linear-gradient(90deg,rgba(255,0,0,0.06),rgba(0,255,0,0.02),rgba(0,0,255,0.06))] z-10 bg-[length:100%_2px,3px_100%] opacity-20"></div>

      {/* Output Area */}
      <div className="flex-1 overflow-y-auto pr-2 custom-scrollbar space-y-1 mb-20 relative z-0">
        <AnimatePresence initial={false}>
          {messages.map((msg) => (
            <motion.div
              key={msg.id}
              initial={{ opacity: 0, x: -10 }}
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

      {/* Input Area + Floating Menu */}
      <div className="absolute bottom-0 left-0 w-full px-4 pb-4 pt-2 bg-gradient-to-t from-[#111] to-transparent z-20">
        
        {/* The Dynamic Menu - Floats above input */}
        <div className="mb-2">
             <CommandMenu input={input} onSelect={handleMenuSelect} context={context} />
        </div>

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