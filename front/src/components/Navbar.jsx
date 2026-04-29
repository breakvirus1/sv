import { AppBar, Toolbar, Typography, Button, Box, IconButton, Avatar, Menu, MenuItem, useMediaQuery } from '@mui/material';
import { useAuth } from '../context/AuthContext';
import { useState } from 'react';
import { Person, Logout, Add, AdminPanelSettings } from '@mui/icons-material';
import { useWindowsStore } from '../store/windowsStore';
import { useNavigate } from 'react-router-dom';
import CreateOrderForm from './CreateOrderForm';

const Navbar = () => {
  const { user, login, logout, isAuthenticated } = useAuth();
  const [anchorEl, setAnchorEl] = useState(null);
  const [createAnchorEl, setCreateAnchorEl] = useState(null);
  const openWindow = useWindowsStore((state) => state.openWindow);
  const navigate = useNavigate();

  const handleMenu = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleCreateMenu = (event) => {
    setCreateAnchorEl(event.currentTarget);
  };

  const handleCloseCreate = () => {
    setCreateAnchorEl(null);
  };

  const handleCreateOrder = () => {
    handleCloseCreate();
    openWindow({
      title: 'Новый заказ',
      x: 100,
      y: 100,
      width: 800,
      height: 600,
      Component: CreateOrderForm,
      props: {}
    });
  };

  return (
    <AppBar position="static">
      <Toolbar>
        <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
          Print SV — Производство
        </Typography>

        {isAuthenticated ? (
          <Box display="flex" alignItems="center" gap={2}>
            {/* Admin Panel button - only for ADMIN role */}
            {user?.roles?.includes('ROLE_ADMIN') && (
              <Button
                variant="outlined"
                color="inherit"
                size="small"
                onClick={() => navigate('/admin')}
                startIcon={<AdminPanelSettings />}
                sx={{ borderColor: 'rgba(255,255,255,0.5)' }}
              >
                Admin Panel
              </Button>
            )}

            {/* Create Order button with dropdown */}
            <Button
              variant="contained"
              color="secondary"
              size="small"
              onClick={handleCreateMenu}
              startIcon={<Add />}
            >
              Создать заказ
            </Button>
            <Menu
              anchorEl={createAnchorEl}
              open={Boolean(createAnchorEl)}
              onClose={handleCloseCreate}
              anchorOrigin={{
                vertical: 'bottom',
                horizontal: 'right',
              }}
              transformOrigin={{
                vertical: 'top',
                horizontal: 'right',
              }}
            >
              <MenuItem onClick={handleCreateOrder}>
                <Add fontSize="small" sx={{ mr: 1 }} />
                Создать заказ
              </MenuItem>
            </Menu>

            <Typography variant="body2">
              {user?.name} ({user?.roles?.join(', ')})
            </Typography>
            <IconButton onClick={handleMenu} color="inherit" size="small">
              <Avatar sx={{ width: 32, height: 32, bgcolor: 'secondary.main' }}>
                {user?.name?.charAt(0)?.toUpperCase() || 'U'}
              </Avatar>
            </IconButton>
            <Menu
              anchorEl={anchorEl}
              open={Boolean(anchorEl)}
              onClose={handleClose}
              anchorOrigin={{
                vertical: 'bottom',
                horizontal: 'right',
              }}
              transformOrigin={{
                vertical: 'top',
                horizontal: 'right',
              }}
            >
              <MenuItem onClick={() => { handleClose(); logout(); }}>
                <Logout fontSize="small" sx={{ mr: 1 }} />
                Выход
              </MenuItem>
            </Menu>
          </Box>
        ) : (
          <Button color="inherit" onClick={login}>
            Войти
          </Button>
        )}
      </Toolbar>
    </AppBar>
  );
};

export default Navbar;
