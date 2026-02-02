import React, { useMemo } from 'react';
import { motion, AnimatePresence, Variants } from 'framer-motion';
import { MenuOption, SystemContext } from '../types';
import { getCommandSuggestions } from '../constants';

interface CommandMenuProps {
  input: string;
  onSelect: (value: string) => void;
  context: SystemContext;
}

// --- Animation Constants ---

const ANIM_DURATION = 0.08; // Very fast
const STAGGER = 0.03;

const listVariants: Variants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: STAGGER,
      when: "beforeChildren" 
    }
  },
  exit: {
    opacity: 0,
    transition: {
      staggerChildren: STAGGER,
      staggerDirection: -1,
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
  
  // Use shared logic for suggestions, passing context to filter based on OS
  const visibleOptions = useMemo(() => getCommandSuggestions(input, context), [input, context]);

  if (visibleOptions.length === 0) return null;

  // Limit display to keep it compact
  const displayOptions = visibleOptions.slice(0, 10);

  return (
    <div className="flex flex-col-reverse items-start gap-1 w-full overflow-hidden">
        <AnimatePresence mode="popLayout">
            <motion.div 
                key={input.trim() + context.os} // Re-render list on input or OS change
                className="flex flex-wrap gap-2 items-end content-end"
                variants={listVariants}
                initial="hidden"
                animate="visible"
                exit="exit"
            >
                {displayOptions.map((opt) => (
                    <OptionItem key={opt.id} option={opt} onClick={() => onSelect(opt.value)} />
                ))}
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

const OptionItem: React.FC<{ option: MenuOption, onClick: () => void }> = ({ option, onClick }) => {
    return (
        <motion.button
            variants={itemVariants}
            onClick={onClick}
            whileHover={{ scale: 1.05, y: -2 }}
            whileTap={{ scale: 0.95 }}
            className={`
                h-8 px-3 
                flex items-center justify-center 
                rounded-md shadow-md border-b-2 border-black/20
                ${option.color}
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