import { AppBar, Toolbar, Typography, Button, Box, IconButton, Avatar, Menu, MenuItem, useMediaQuery, ListItemIcon, ListItemText, Chip, Divider } from '@mui/material';
import { useAuth } from '../context/AuthContext';
import { useState } from 'react';
import { Person, Logout, Add, AdminPanelSettings, ShoppingBag, ArrowDropDown, Assignment } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '../services/api';
import { getStatusColor, getStatusLabel } from '../utils/orderUtils';

const statusGroups = [
  { key: 'DRAFT', label: 'Черновики' },
  { key: 'APPROVAL', label: 'На согласовании' },
  { key: 'IN_PROGRESS', label: 'В работе' },
  { key: 'READY', label: 'Готовы' },
];

const Navbar = () => {
  const { user, login, logout, isAuthenticated } = useAuth();
  const [anchorEl, setAnchorEl] = useState(null);
  const [createAnchorEl, setCreateAnchorEl] = useState(null);
  const [ordersAnchorEl, setOrdersAnchorEl] = useState(null);
  const navigate = useNavigate();

  const { data: orders = [] } = useQuery({
    queryKey: ['navbarOrders'],
    queryFn: async () => {
      const response = await api.get('/api/v1/orders?size=50');
      return response.data.content || [];
    },
    enabled: isAuthenticated,
    retry: 1,
    retryDelay: 1000,
  });

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

  const handleOrdersMenu = (event) => {
    setOrdersAnchorEl(event.currentTarget);
  };

  const handleCloseOrders = () => {
    setOrdersAnchorEl(null);
  };

  const handleCreateOrder = () => {
    handleCloseCreate();
    navigate('/orders/new');
  };

  const handleStatusClick = (status) => {
    handleCloseOrders();
    navigate(`/orders?status=${status}`);
  };

  const isManager = user?.roles?.includes('ROLE_MANAGER') || user?.roles?.includes('ROLE_ADMIN');

  const handleMyOrders = () => {
    handleCloseOrders();
    navigate(isManager ? '/manager' : '/orders?my=true');
  };

  const handleAllOrders = () => {
    handleCloseOrders();
    navigate('/orders');
  };

  return (
    <AppBar position="static">
      <Toolbar>
        <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
          Print SV — Производство
        </Typography>

        {isAuthenticated ? (
          <Box display="flex" alignItems="center" gap={2}>
            {/* Orders dropdown button */}
            <Button
              variant="outlined"
              color="inherit"
              size="small"
              onClick={handleOrdersMenu}
              startIcon={<ShoppingBag />}
              endIcon={<ArrowDropDown />}
              sx={{ borderColor: 'rgba(255,255,255,0.5)' }}
            >
              Заказы
            </Button>
            <Menu
              anchorEl={ordersAnchorEl}
              open={Boolean(ordersAnchorEl)}
              onClose={handleCloseOrders}
              PaperProps={{ sx: { minWidth: 280 } }}
            >
              <MenuItem onClick={handleAllOrders}>
                <ListItemIcon>
                  <ShoppingBag fontSize="small" />
                </ListItemIcon>
                <ListItemText primary="Все заказы" />
              </MenuItem>
              <Divider />
              {statusGroups.map((group) => (
                <MenuItem
                  key={group.key}
                  onClick={() => handleStatusClick(group.key)}
                >
                  <ListItemIcon sx={{ color: getStatusColor(group.key) }}>
                    <Assignment fontSize="small" />
                  </ListItemIcon>
                  <ListItemText primary={group.label} />
                </MenuItem>
              ))}
              <Divider />
              <MenuItem onClick={handleMyOrders}>
                <Box display="flex" alignItems="center" gap={1}>
                  <ListItemIcon>
                    <Person fontSize="small" />
                  </ListItemIcon>
                  <ListItemText primary="Мои заказы" />
                </Box>
              </MenuItem>
            </Menu>

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
              {user?.name} ({user?.roles?.map(r => r.replace(/^ROLE_/, '')).join(', ')})
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
