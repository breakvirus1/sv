import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  Button
} from '@mui/material';

const statusOptions = ['WAITING', 'LAUNCHED', 'IN_PROGRESS', 'READY', 'ACCEPTED', 'CLOSED'];

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
          {statusOptions.map((status) => (
            <MenuItem key={status} value={status}>
              {status}
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
