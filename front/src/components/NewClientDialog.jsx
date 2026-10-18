import { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  Alert
} from '@mui/material';

const INITIAL_FORM = {
  name: '',
  type: 'PRIVATE',
  contactPerson: '',
  phone: '',
  email: '',
  priceplus: ''
};

const NewClientDialog = ({ open, onClose, onSubmit, isLoading, fieldErrors = {}, errorMessage = null }) => {
  const [formData, setFormData] = useState(INITIAL_FORM);

  const handleChange = (field) => (e) => {
    setFormData(prev => ({ ...prev, [field]: e.target.value }));
  };

  const handleSubmit = () => {
    onSubmit(formData);
  };

  const handleClose = () => {
    setFormData(INITIAL_FORM);
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Новый клиент</DialogTitle>
      <DialogContent>
        {errorMessage && <Alert severity="error" sx={{ mt: 1, mb: 1 }}>{errorMessage}</Alert>}
        <TextField
          autoFocus
          fullWidth
          margin="dense"
          label="Название"
          required
          value={formData.name}
          onChange={handleChange('name')}
          error={!!fieldErrors.name}
          helperText={fieldErrors.name}
        />
        <FormControl fullWidth margin="dense">
          <InputLabel>Тип</InputLabel>
          <Select
            value={formData.type}
            label="Тип"
            onChange={handleChange('type')}
          >
            <MenuItem value="PRIVATE">Частник</MenuItem>
            <MenuItem value="COMPANY">Компания</MenuItem>
          </Select>
        </FormControl>
        <TextField
          fullWidth
          margin="dense"
          label="Контактное лицо"
          value={formData.contactPerson}
          onChange={handleChange('contactPerson')}
        />
        <TextField
          fullWidth
          margin="dense"
          label="Телефон"
          value={formData.phone}
          onChange={handleChange('phone')}
          error={!!fieldErrors.phone}
          helperText={fieldErrors.phone}
        />
        <TextField
          fullWidth
          margin="dense"
          label="Email"
          value={formData.email}
          onChange={handleChange('email')}
          error={!!fieldErrors.email}
          helperText={fieldErrors.email}
        />
        <TextField
          fullWidth
          margin="dense"
          label="Процент добавки (priceplus)"
          type="number"
          value={formData.priceplus}
          onChange={handleChange('priceplus')}
          inputProps={{ min: 0, step: 0.01 }}
          error={!!fieldErrors.priceplus}
          helperText={fieldErrors.priceplus}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>Отмена</Button>
        <Button onClick={handleSubmit} variant="contained" disabled={isLoading}>
          {isLoading ? 'Сохранение...' : 'Сохранить'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default NewClientDialog;
