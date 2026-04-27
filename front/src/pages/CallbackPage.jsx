import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, CircularProgress, Typography } from '@mui/material';
import { useAuth } from '../context/AuthContext';

const CallbackPage = () => {
  const navigate = useNavigate();
  const { handleCallback } = useAuth();

  useEffect(() => {
    const processAuth = async () => {
      try {
        await handleCallback();
        navigate('/orders', { replace: true });
      } catch (error) {
        console.error('Auth callback error:', error);
        navigate('/login', { replace: true });
      }
    };

    processAuth();
  }, [handleCallback, navigate]);

  return (
    <Box
      display="flex"
      flexDirection="column"
      alignItems="center"
      justifyContent="center"
      minHeight="100vh"
    >
      <CircularProgress size={60} />
      <Typography variant="h6" sx={{ mt: 3 }}>
        Авторизация...
      </Typography>
    </Box>
  );
};

export default CallbackPage;
