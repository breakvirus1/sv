import React from 'react';
import { useWindowsStore } from '../store/windowsStore';
import { X, Minus, Square } from 'lucide-react';

export default function Window({ windowData }) {
  const { closeWindow, minimizeWindow, maximizeWindow, bringToFront, updatePositionAndSize } = useWindowsStore();
  const isMaximized = windowData.maximized;

  const handleMouseDown = (e) => {
    bringToFront(windowData.id);
    
    const startX = e.clientX;
    const startY = e.clientY;
    const startLeft = windowData.x;
    const startTop = windowData.y;

    const handleMouseMove = (e) => {
      const newX = startLeft + (e.clientX - startX);
      const newY = startTop + (e.clientY - startY);
      updatePositionAndSize(windowData.id, newX, newY, windowData.width, windowData.height);
    };

    const handleMouseUp = () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
    };

    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
  };

  const handleResizeMouseDown = (e) => {
    e.stopPropagation();
    const startX = e.clientX;
    const startY = e.clientY;
    const startWidth = windowData.width;
    const startHeight = windowData.height;
    const startLeft = windowData.x;
    const startTop = windowData.y;

    const handleMouseMove = (e) => {
      const newWidth = Math.max(400, startWidth + (e.clientX - startX));
      const newHeight = Math.max(300, startHeight + (e.clientY - startY));
      updatePositionAndSize(windowData.id, startLeft, startTop, newWidth, newHeight);
    };

    const handleMouseUp = () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
    };

    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
  };

  if (windowData.minimized) return null;

  return (
    <div
      className="fixed bg-white rounded-lg shadow-[4px_4px_10px_rgba(0,0,0,0.3)] select-none"
      style={{
        left: windowData.x,
        top: windowData.y,
        width: isMaximized ? 'calc(100vw - 2rem)' : windowData.width,
        height: isMaximized ? 'calc(100vh - 6rem)' : windowData.height,
        zIndex: windowData.zIndex,
      }}
    >
      {/* Title Bar */}
      <div
        className="h-9 bg-gradient-to-b from-[#0055ea] to-[#2b8cff] flex items-center px-2 cursor-move border-t border-[#7aa7f5] rounded-t-lg"
        onMouseDown={handleMouseDown}
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
            <Square size={14} />
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
      <div className="flex-1 overflow-auto bg-[#f0f0f0] rounded-b-lg">
        {windowData.element}
      </div>

      {/* Resize handle */}
      {!isMaximized && (
        <div
          className="absolute bottom-0 right-0 w-5 h-5 cursor-se-resize"
          style={{
            borderRight: '2px solid #ccc',
            borderBottom: '2px solid #ccc',
          }}
          onMouseDown={handleResizeMouseDown}
        />
      )}
    </div>
  );
}
