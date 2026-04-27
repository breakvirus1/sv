import { AppBar, Toolbar, Typography, Button, Box, IconButton, Avatar, Menu, MenuItem } from '@mui/material';
import { useAuth } from '../context/AuthContext';
import { useState } from 'react';
import { Person, Logout } from '@mui/icons-material';

const Navbar = () => {
  const { user, login, logout, isAuthenticated } = useAuth();
  const [anchorEl, setAnchorEl] = useState(null);

  const handleMenu = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  return (
    <AppBar position="static">
      <Toolbar>
        <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
          Print SV — Производство
        </Typography>

        {isAuthenticated ? (
          <Box display="flex" alignItems="center" gap={2}>
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
