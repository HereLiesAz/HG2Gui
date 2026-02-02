/**
 * @file CommandMenu.tsx
 * @description Renders a floating list of valid next-commands based on the user's input.
 * Implements "wonky" physics via Framer Motion for entrance/exit animations.
 */

import React, { useMemo } from 'react';
import { motion, AnimatePresence, Variants } from 'framer-motion';
import { MenuOption, SystemContext } from '../types';
import { getCommandSuggestions } from '../constants';

interface CommandMenuProps {
  input: string;                    // Current input string to analyze
  onSelect: (value: string) => void; // Callback when an option is clicked
  context: SystemContext;           // OS Context for filtering valid commands
}

// --- Animation Configuration ---
// These control the "pop" and "stagger" feel of the menu items.
const ANIM_DURATION = 0.08; // Very fast, snappy movement
const STAGGER = 0.03;       // Delay between each item appearing

const listVariants: Variants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: STAGGER, // Items appear one after another
      when: "beforeChildren" 
    }
  },
  exit: {
    opacity: 0,
    transition: {
      staggerChildren: STAGGER,
      staggerDirection: -1, // Items disappear in reverse order
      when: "afterChildren",
      duration: 0.1
    }
  }
};

const itemVariants: Variants = {
  hidden: { y: 10, opacity: 0, scale: 0.9 },
  visible: {
    y: 0,
    opacity: 1,
    scale: 1,
    transition: { type: "tween", ease: "circOut", duration: ANIM_DURATION }
  },
  exit: {
    y: 5,
    opacity: 0,
    scale: 0.9,
    transition: { duration: ANIM_DURATION }
  }
};

const CommandMenu: React.FC<CommandMenuProps> = ({ input, onSelect, context }) => {
  
  // Memoize suggestions calculation to prevent re-calc on every render
  // Filters commands based on what is available in the current OS context.
  const visibleOptions = useMemo(() => getCommandSuggestions(input, context), [input, context]);

  // Don't render anything if there are no valid next steps
  if (visibleOptions.length === 0) return null;

  // Cap the number of items to prevent UI clutter
  const displayOptions = visibleOptions.slice(0, 10);

  return (
    <div className="flex flex-col-reverse items-start gap-1 w-full overflow-hidden">
        <AnimatePresence mode="popLayout">
            <motion.div 
                // Using a key based on input+OS forces the list to re-mount animation on change
                key={input.trim() + context.os} 
                className="flex flex-wrap gap-2 items-end content-end"
                variants={listVariants}
                initial="hidden"
                animate="visible"
                exit="exit"
            >
                {displayOptions.map((opt) => (
                    <OptionItem key={opt.id} option={opt} onClick={() => onSelect(opt.value)} />
                ))}
                {/* Visual indicator if there are more commands than shown */}
                {visibleOptions.length > 10 && (
                     <motion.div variants={itemVariants} className="h-8 px-2 flex items-center text-xs text-guide-green opacity-50 font-mono">
                        +{visibleOptions.length - 10}
                     </motion.div>
                )}
            </motion.div>
        </AnimatePresence>
    </div>
  );
};

/**
 * Individual Button Component for a menu option.
 */
const OptionItem: React.FC<{ option: MenuOption, onClick: () => void }> = ({ option, onClick }) => {
    return (
        <motion.button
            variants={itemVariants}
            onClick={onClick}
            whileHover={{ scale: 1.05, y: -2 }} // Tactile hover effect
            whileTap={{ scale: 0.95 }}
            className={`
                h-8 px-3 
                flex items-center justify-center 
                rounded-md shadow-md border-b-2 border-black/20
                ${option.color} // Dynamic color from command definition
                transition-colors
            `}
        >
            <span className="font-mono font-bold text-xs text-guide-black tracking-wider uppercase">
                {option.label}
            </span>
        </motion.button>
    );
}

export default CommandMenu;