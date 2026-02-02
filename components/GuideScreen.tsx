/**
 * @file GuideScreen.tsx
 * @description Displays the "The Guide" manual entries.
 * This component visualizes the structured data returned by the Gemini AI.
 */

import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { GuideData, SystemContext } from '../types';
import { AlertTriangle, Rocket, Fish, Globe, Bot, X } from 'lucide-react';
import CommandMenu from './CommandMenu';

interface GuideScreenProps {
  data: GuideData | null;           // The loaded manual entry (or null if empty)
  loading: boolean;                 // Loading state for API calls
  onCommandSelect: (cmd: string) => void; 
  onClearData: () => void;          // Callback to close the entry
  context: SystemContext;
}

const GuideScreen: React.FC<GuideScreenProps> = ({ data, loading, onCommandSelect, onClearData, context }) => {
  return (
    <div className="h-full flex flex-col items-center justify-center bg-guide-offWhite relative overflow-hidden">
      {/* Background Grid Pattern for retro-technical feel */}
      <div className="absolute inset-0 opacity-10 pointer-events-none" 
           style={{ backgroundImage: 'radial-gradient(#000 1px, transparent 1px)', backgroundSize: '20px 20px' }}>
      </div>

      <AnimatePresence mode="wait">
        {loading ? (
          <LoadingState key="loading" />
        ) : data ? (
          <ContentState key="content" data={data} onClear={onClearData} />
        ) : (
          // If no data, show the command menu in the center (Landing state)
          <div key="menu" className="w-full h-full z-10">
              <CommandMenu input="" onSelect={onCommandSelect} context={context} />
          </div>
        )}
      </AnimatePresence>
    </div>
  );
};

/**
 * Displays a whimsical loading animation ("Don't Panic" logo style).
 */
const LoadingState = () => (
  <motion.div
    initial={{ opacity: 0 }}
    animate={{ opacity: 1 }}
    exit={{ opacity: 0, y: -20 }}
    className="flex flex-col items-center z-20"
  >
    <motion.div
      animate={{ 
        rotate: 360,
        scale: [1, 1.2, 1],
      }}
      transition={{ 
        rotate: { repeat: Infinity, duration: 2, ease: "linear" },
        scale: { repeat: Infinity, duration: 1 }
      }}
      className="mb-6"
    >
      <div className="w-24 h-24 border-8 border-guide-black rounded-full border-t-transparent" />
    </motion.div>
    <motion.h3 
      animate={{ opacity: [0.5, 1, 0.5] }}
      transition={{ repeat: Infinity, duration: 1.5 }}
      className="text-xl font-bold tracking-widest text-guide-black"
    >
      CALCULATING...
    </motion.h3>
  </motion.div>
);

/**
 * Displays the actual manual content card.
 */
const ContentState: React.FC<{ data: GuideData; onClear: () => void }> = ({ data, onClear }) => {
  return (
    <motion.div
      initial={{ y: 50, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      exit={{ y: 50, opacity: 0 }}
      transition={{ type: "spring", stiffness: 200, damping: 15 }}
      className="w-full h-full flex flex-col p-6 relative z-10"
    >
      {/* Close Button */}
      <button 
        onClick={onClear}
        className="absolute top-4 right-4 p-2 bg-guide-black/10 rounded-full hover:bg-guide-black/20 transition-colors z-50"
      >
        <X size={24} className="text-guide-black" />
      </button>

      {/* Dynamic Icon */}
      <div className="flex-1 flex flex-col items-center justify-center mb-6 relative">
        <VisualIcon type={data.visualType} />
      </div>

      {/* Info Card */}
      <motion.div 
        className="bg-white/50 p-6 rounded-2xl border-2 border-guide-black/10 shadow-xl backdrop-blur-sm"
        initial={{ scaleX: 0 }}
        animate={{ scaleX: 1 }}
        transition={{ delay: 0.2, type: "spring" }}
      >
        <div className="flex justify-between items-start mb-2">
          <motion.h1 
            className="text-3xl font-bold uppercase text-guide-darkRed font-rounded"
            initial={{ x: -20, opacity: 0 }}
            animate={{ x: 0, opacity: 1 }}
            transition={{ delay: 0.3 }}
          >
            {data.title}
          </motion.h1>
          <div className="flex flex-col items-end">
             <span className="text-xs font-bold uppercase opacity-50 text-guide-black">Danger Level</span>
             <span className={`text-sm font-bold px-2 py-1 rounded ${getDangerColor(data.dangerLevel)} text-white`}>
               {data.dangerLevel.toUpperCase()}
             </span>
          </div>
        </div>
        
        <motion.p 
          className="text-lg leading-relaxed font-medium text-guide-black font-rounded"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.5 }}
        >
          {data.description}
        </motion.p>
      </motion.div>
    </motion.div>
  );
};

/**
 * Maps the AI-returned `visualType` string to a Lucide React icon with animation.
 */
const VisualIcon: React.FC<{ type: string }> = ({ type }) => {
  const commonProps = { 
    size: 140, 
    strokeWidth: 1.5,
    className: "drop-shadow-lg"
  };

  const wrapperProps = {
    initial: { scale: 0, rotate: -45 },
    animate: { scale: 1, rotate: 0 },
    transition: { type: "spring" as const, bounce: 0.5, duration: 1 }
  };

  switch (type) {
    case 'robot':
      return <motion.div {...wrapperProps}><Bot {...commonProps} className="text-guide-darkRed" /></motion.div>;
    case 'fish':
      return <motion.div {...wrapperProps}><Fish {...commonProps} className="text-guide-orange" /></motion.div>;
    case 'ship':
      return <motion.div {...wrapperProps}><Rocket {...commonProps} className="text-guide-black" /></motion.div>;
    case 'planet':
      return <motion.div {...wrapperProps}><Globe {...commonProps} className="text-guide-cyan invert" /></motion.div>;
    default:
      return <motion.div {...wrapperProps}><AlertTriangle {...commonProps} className="text-guide-darkRed" /></motion.div>;
  }
};

const getDangerColor = (level: string) => {
  switch(level.toLowerCase()) {
    case 'mostly harmless': return 'bg-guide-green';
    case 'low': return 'bg-guide-green';
    case 'medium': return 'bg-guide-orange';
    case 'high': return 'bg-guide-darkRed';
    case 'extreme': return 'bg-black';
    default: return 'bg-guide-gray';
  }
};

export default GuideScreen;