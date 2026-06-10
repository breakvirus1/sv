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
  Button
} from '@mui/material';

const NewClientDialog = ({ open, onClose, onSubmit, isLoading }) => {
  const [formData, setFormData] = useState({
    name: '',
    type: 'PRIVATE',
    contactPerson: '',
    phone: '',
    email: '',
    priceplus: ''
  });

  const handleChange = (field) => (e) => {
    setFormData(prev => ({ ...prev, [field]: e.target.value }));
  };

  const handleSubmit = () => {
    onSubmit(formData);
  };

  const handleClose = () => {
    setFormData({
      name: '',
      type: 'PRIVATE',
      contactPerson: '',
      phone: '',
      email: '',
      priceplus: ''
    });
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Новый клиент</DialogTitle>
      <DialogContent>
        <TextField
          autoFocus
          fullWidth
          margin="dense"
          label="Название"
          value={formData.name}
          onChange={handleChange('name')}
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
        />
        <TextField
          fullWidth
          margin="dense"
          label="Email"
          value={formData.email}
          onChange={handleChange('email')}
        />
        <TextField
          fullWidth
          margin="dense"
          label="Процент добавки (priceplus)"
          type="number"
          value={formData.priceplus}
          onChange={handleChange('priceplus')}
          inputProps={{ min: 0, step: 0.01 }}
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
