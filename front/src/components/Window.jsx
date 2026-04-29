import React from 'react';
import Draggable from 'react-draggable';
import { Resizable } from 'react-resizable';
import { useWindowsStore } from '../store/windowsStore';
import { X, Minus, Square, SquareCheck } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { Paper } from '@mui/material';

export default function Window({ windowData }) {
  const { closeWindow, minimizeWindow, maximizeWindow, bringToFront, updatePositionAndSize } = useWindowsStore();
  const isMaximized = windowData.maximized;
  const [localPos, setLocalPos] = useState({ x: windowData.x, y: windowData.y });
  const [localSize, setLocalSize] = useState({ w: windowData.width, h: windowData.height });
  const [isDragging, setIsDragging] = useState(false);

  const wrapperRef = useRef(null);
  const dragRef = useRef(null);
  const lastSynced = useRef({ x: windowData.x, y: windowData.y, w: windowData.width, h: windowData.height });

  // Синхронизация позиции/размера после драга/ресайза
  const handleDragStop = (e, d) => {
    setIsDragging(false);
    const newX = d.x;
    const newY = d.y;
    lastSynced.current = { ...lastSynced.current, x: newX, y: newY };
    updatePositionAndSize(windowData.id, newX, newY, localSize.w, localSize.h);
    setLocalPos({ x: newX, y: newY });
  };

  const handleDragStart = () => {
    setIsDragging(true);
    bringToFront(windowData.id);
  };

  const handleResizeStop = (e, _, element, _delta, position) => {
    const { width, height } = element.getBoundingClientRect();
    const newW = width;
    const newH = height;
    const newX = position.x;
    const newY = position.y;
    lastSynced.current = { x: newX, y: newY, w: newW, h: newH };
    updatePositionAndSize(windowData.id, newX, newY, newW, newH);
    setLocalPos({ x: newX, y: newY });
    setLocalSize({ w: newW, h: newH });
  };

  if (windowData.minimized) return null;

  const baseClasses = "bg-white rounded-lg shadow-[4px_4px_10px_rgba(0,0,0,0.3)] select-none";
  const maximizeClass = isMaximized ? "maximized-window" : "";
  const windowClasses = `${baseClasses} ${maximizeClass}`.trim();

  return (
    <div
      style={{
        position: 'absolute',
        left: 0,
        top: 0,
        zIndex: windowData.zIndex,
        width: localSize.w,
        height: localSize.h,
        transform: `translate(${localPos.x}px, ${localPos.y}px)`
      }}
      ref={wrapperRef}
    >
      <Draggable
        nodeRef={dragRef}
        handle=".window-title-bar"
        position={{ x: 0, y: 0 }}
        onStop={handleDragStop}
        onStart={handleDragStart}
        disabled={isMaximized}
      >
        <div ref={dragRef} style={{ width: '100%', height: '100%' }}>
          <Paper
            elevation={0}
            className={windowClasses}
            style={{
              width: '100%',
              height: '100%',
              display: 'flex',
              flexDirection: 'column',
              overflow: 'hidden',
              borderRadius: 8
            }}
          >
            {/* Title Bar */}
            <div
              className="window-title-bar h-9 bg-gradient-to-b from-[#0055ea] to-[#2b8cff] flex items-center px-2 cursor-move active:cursor-grabbing border-t border-[#7aa7f5]"
              onDoubleClick={() => maximizeWindow(windowData.id)}
            >
              <div className="flex-1 font-medium text-sm text-white flex items-center gap-2 truncate">
                {windowData.title}
              </div>
              <div className="flex items-center gap-0.5">
                <button
                  onClick={() => minimizeWindow(windowData.id)}
                  className="hover:bg-white/20 p-1 rounded text-white"
                  title="Свернуть"
                >
                  <Minus size={14} />
                </button>
                <button
                  onClick={() => maximizeWindow(windowData.id)}
                  className="hover:bg-white/20 p-1 rounded text-white"
                  title={isMaximized ? "Восстановить" : "Развернуть"}
                >
                  {isMaximized ? <SquareCheck size={14} /> : <Square size={14} />}
                </button>
                <button
                  onClick={() => closeWindow(windowData.id)}
                  className="hover:bg-red-600 hover:text-white p-1 rounded text-white"
                  title="Закрыть"
                >
                  <X size={14} />
                </button>
              </div>
            </div>

            {/* Content */}
            <div className="flex-1 overflow-auto bg-[#f0f0f0]">
              {windowData.element}
            </div>
          </Paper>
        </div>
      </Draggable>

      {/* Resizable handle */}
      {!isMaximized && (
        <Resizable
          width={localSize.w}
          height={localSize.h}
          minConstraints={[400, 300]}
          maxConstraints={['100%', '100%']}
          handle={
            <div
              style={{
                position: 'absolute',
                right: 0,
                bottom: 0,
                width: 20,
                height: 20,
                cursor: 'se-resize',
                zIndex: 1000
              }}
            />
          }
          onResizeStop={handleResizeStop}
        >
          <div style={{ width: '100%', height: '100%' }} />
        </Resizable>
      )}
    </div>
  );
}
