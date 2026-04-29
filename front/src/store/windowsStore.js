import { create } from 'zustand';
import React from 'react';

export const useWindowsStore = create((set, get) => ({
  windows: [],
  nextZIndex: 10,

  openWindow: (newWin) => {
    const id = 'win-' + Date.now();
    const zIndex = get().nextZIndex;

    const { Component, props: userProps = {}, ...rest } = newWin;

    // Создаём React-элемент сразу, добавляя windowId и closeWindow
    let element = null;
    if (React.isValidElement(Component)) {
      element = React.cloneElement(Component, { ...userProps, windowId: id, closeWindow: () => get().closeWindow(id) });
    } else if (typeof Component === 'function' || typeof Component === 'string') {
      element = React.createElement(Component, { ...userProps, windowId: id, closeWindow: () => get().closeWindow(id) });
    } else {
      console.error('Invalid Component type:', Component);
    }

    const win = {
      ...rest,
      id,
      zIndex,
      minimized: false,
      maximized: false,
      element,
    };

    set((state) => ({
      windows: [...state.windows, win],
      nextZIndex: zIndex + 1,
    }));
  },

  closeWindow: (id) =>
    set((state) => ({
      windows: state.windows.filter((w) => w.id !== id),
    })),

  minimizeWindow: (id) =>
    set((state) => ({
      windows: state.windows.map((w) =>
        w.id === id ? { ...w, minimized: true } : w
      ),
    })),

  maximizeWindow: (id) =>
    set((state) => ({
      windows: state.windows.map((w) =>
        w.id === id ? { ...w, maximized: !w.maximized } : w
      ),
    })),

  bringToFront: (id) =>
    set((state) => {
      const maxZ = Math.max(...state.windows.map((w) => w.zIndex), 9);
      return {
        windows: state.windows.map((w) =>
          w.id === id ? { ...w, zIndex: maxZ + 1 } : w
        ),
        nextZIndex: maxZ + 2,
      };
    }),

  updatePositionAndSize: (id, x, y, width, height) =>
    set((state) => ({
      windows: state.windows.map((w) =>
        w.id === id ? { ...w, x, y, width, height } : w
      ),
    })),

  restoreWindow: (id) =>
    set((state) => ({
      windows: state.windows.map((w) =>
        w.id === id ? { ...w, maximized: false } : w
      ),
    })),
}));
