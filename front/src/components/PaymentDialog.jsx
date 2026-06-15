import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  Button
} from '@mui/material';
import { useState } from 'react';

const PaymentDialog = ({ open, onClose, onSubmit }) => {
  const [formData, setFormData] = useState({
    amount: '',
    paymentDate: new Date().toISOString().split('T')[0],
    paymentType: 'Безнал',
    details: ''
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit({
      ...formData,
      amount: parseFloat(formData.amount)
    });
  };

  const handleClose = () => {
    setFormData({
      amount: '',
      paymentDate: new Date().toISOString().split('T')[0],
      paymentType: 'Безнал',
      details: ''
    });
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Добавить оплату</DialogTitle>
      <DialogContent>
        <form onSubmit={handleSubmit}>
          <TextField
            fullWidth
            margin="dense"
            label="Сумма"
            name="amount"
            type="number"
            value={formData.amount}
            onChange={handleChange}
            required
            inputProps={{ step: 0.01 }}
          />
          <TextField
            fullWidth
            margin="dense"
            label="Дата оплаты"
            name="paymentDate"
            type="date"
            value={formData.paymentDate}
            onChange={handleChange}
            required
            InputLabelProps={{ shrink: true }}
          />
          <TextField
            fullWidth
            margin="dense"
            label="Вид оплаты"
            name="paymentType"
            select
            value={formData.paymentType}
            onChange={handleChange}
          >
            <MenuItem value="Безнал">Безнал</MenuItem>
            <MenuItem value="Нал">Нал</MenuItem>
            <MenuItem value="Карта">Карта</MenuItem>
          </TextField>
          <TextField
            fullWidth
            margin="dense"
            label="Примечание"
            name="details"
            multiline
            rows={2}
            value={formData.details}
            onChange={handleChange}
          />
          <DialogActions sx={{ mt: 2 }}>
            <Button type="button" onClick={handleClose}>Отмена</Button>
            <Button type="submit" variant="contained">Добавить</Button>
          </DialogActions>
        </form>
      </DialogContent>
    </Dialog>
  );
};

export default PaymentDialog;
