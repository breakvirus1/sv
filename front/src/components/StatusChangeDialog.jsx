import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  Button
} from '@mui/material';

const statusOptions = [
  { value: 'DRAFT', label: 'Черновик' },
  { value: 'APPROVAL', label: 'Согласование' },
  { value: 'IN_PROGRESS', label: 'В работе' },
  { value: 'READY', label: 'Готов' }
];

const StatusChangeDialog = ({ open, onClose, onSave, currentStatus }) => {
  return (
    <Dialog open={open} onClose={onClose}>
      <DialogTitle>Изменить статус заказа</DialogTitle>
      <DialogContent>
        <TextField
          select
          fullWidth
          margin="dense"
          label="Статус"
          defaultValue={currentStatus}
          onChange={(e) => onSave(e.target.value)}
        >
          {statusOptions.map((option) => (
            <MenuItem key={option.value} value={option.value}>
              {option.label}
            </MenuItem>
          ))}
        </TextField>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Отмена</Button>
        <Button onClick={() => {}} variant="contained">
          Сохранить
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default StatusChangeDialog;
