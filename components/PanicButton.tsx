/**
 * @file PanicButton.tsx
 * @description A humorous, tactile button component inspired by the "DON'T PANIC" branding.
 * Used to trigger system resets or view changes.
 */

import React from 'react';
import { motion } from 'framer-motion';

interface PanicButtonProps {
  onPanic: () => void; // Callback when clicked
}

const PanicButton: React.FC<PanicButtonProps> = ({ onPanic }) => {
  return (
    <motion.button
      whileHover={{ scale: 1.05 }}
      whileTap={{ scale: 0.95 }}
      onClick={onPanic}
      className="
        bg-guide-darkRed border-4 border-b-8 border-r-8 border-black 
        rounded-xl px-4 py-2 
        text-white font-rounded font-bold tracking-widest
        shadow-lg active:border-b-4 active:border-r-4 active:translate-y-1 active:translate-x-1
        transition-all
      "
    >
      DON'T PANIC
    </motion.button>
  );
};

export default PanicButton;