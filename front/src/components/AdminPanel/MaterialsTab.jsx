import {
  Box,
  Typography,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Button,
  IconButton,
  TableContainer,
  Paper,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Checkbox,
  FormControlLabel,
  FormGroup,
  CircularProgress,
  Alert,
  TextField
} from '@mui/material';
import { Add, Edit, Delete, CopyAll } from '@mui/icons-material';
import { useState, useEffect } from 'react';
import api from '../../services/api';

const MaterialsTab = ({ materialsData, onAddClick, onEditClick, onDeleteClick, onRefresh }) => {
  const [opsDialogOpen, setOpsDialogOpen] = useState(false);
  const [opsDialogMaterial, setOpsDialogMaterial] = useState(null);
  const [groupedOps, setGroupedOps] = useState([]);
  const [ungroupedOps, setUngroupedOps] = useState([]);
  const [selectedOpIds, setSelectedOpIds] = useState([]);
  const [opsLoading, setOpsLoading] = useState(false);
  const [opsError, setOpsError] = useState(null);

  const [copyDialogOpen, setCopyDialogOpen] = useState(false);
  const [copyMaterial, setCopyMaterial] = useState(null);
  const [copyName, setCopyName] = useState('');
  const [copySelectedOpIds, setCopySelectedOpIds] = useState([]);
  const [copyLoading, setCopyLoading] = useState(false);
  const [copyError, setCopyError] = useState(null);

  const openOpsDialog = async (mat) => {
    setOpsDialogMaterial(mat);
    setSelectedOpIds([]);
    setUngroupedOps([]);
    setOpsError(null);
    setOpsDialogOpen(true);
    setOpsLoading(true);
    try {
      const [groupedRes, mappingsRes] = await Promise.all([
        api.get('/api/v1/calculations/operations/grouped'),
        api.get(`/api/v1/admin/materials/${mat.id}/operation-groups/all`)
      ]);
      const groups = groupedRes.data?.groups || [];
      const ungrouped = groupedRes.data?.ungroupedOperations || [];
      setGroupedOps(groups);
      setUngroupedOps(ungrouped);

      const existingIds = (mappingsRes.data || [])
        .map(m => m.operation?.id)
        .filter(id => id != null);
      setSelectedOpIds(existingIds);
    } catch (e) {
      console.error('Failed to load operations:', e);
      setOpsError('Не удалось загрузить операции');
      setGroupedOps([]);
      setUngroupedOps([]);
    } finally {
      setOpsLoading(false);
    }
  };

  const toggleOp = (opId) => {
    setSelectedOpIds(prev =>
      prev.includes(opId) ? prev.filter(id => id !== opId) : [...prev, opId]
    );
  };

  const handleSaveOps = async () => {
    if (!opsDialogMaterial) return;
    try {
      await api.put(
        `/api/v1/admin/materials/${opsDialogMaterial.id}/operation-groups/replace`,
        { operationIds: selectedOpIds }
      );
      handleCloseOpsDialog();
    } catch (e) {
      console.error('Failed to save operations:', e);
      setOpsError('Ошибка сохранения: ' + (e.response?.data?.message || e.message));
    }
  };

  const handleCloseOpsDialog = () => {
    setOpsDialogOpen(false);
    setOpsDialogMaterial(null);
    setGroupedOps([]);
    setUngroupedOps([]);
    setSelectedOpIds([]);
    setOpsError(null);
  };

  const openCopyDialog = async (mat) => {
    setCopyMaterial(mat);
    setCopyName(`Копия ${mat.name}`);
    setCopySelectedOpIds([]);
    setUngroupedOps([]);
    setCopyError(null);
    setCopyDialogOpen(true);
    setOpsLoading(true);
    try {
      const [groupedRes, mappingsRes] = await Promise.all([
        api.get('/api/v1/calculations/operations/grouped'),
        api.get(`/api/v1/admin/materials/${mat.id}/operation-groups/all`)
      ]);
      const groups = groupedRes.data?.groups || [];
      const ungrouped = groupedRes.data?.ungroupedOperations || [];
      setGroupedOps(groups);
      setUngroupedOps(ungrouped);

      const existingIds = (mappingsRes.data || [])
        .map(m => m.operation?.id)
        .filter(id => id != null);
      setCopySelectedOpIds(existingIds);
    } catch (e) {
      console.error('Failed to load operations:', e);
      setCopyError('Не удалось загрузить операции');
      setGroupedOps([]);
      setUngroupedOps([]);
    } finally {
      setOpsLoading(false);
    }
  };

  const toggleOpCopy = (opId) => {
    setCopySelectedOpIds(prev =>
      prev.includes(opId) ? prev.filter(id => id !== opId) : [...prev, opId]
    );
  };

  const handleCreateCopy = async () => {
    if (!copyMaterial || !copyName.trim()) return;
    setCopyLoading(true);
    setCopyError(null);
    try {
      await api.post('/api/v1/admin/materials-with-operations', {
        name: copyName.trim(),
        unit: copyMaterial.unit,
        price: copyMaterial.price,
        wasteCoefficient: copyMaterial.wasteCoefficient,
        operationIds: copySelectedOpIds
      });
      setCopyDialogOpen(false);
      onRefresh?.();
    } catch (e) {
      console.error('Failed to create copy:', e);
      setCopyError('Ошибка создания: ' + (e.response?.data?.message || e.message));
    } finally {
      setCopyLoading(false);
    }
  };

  const handleCloseCopyDialog = () => {
    setCopyDialogOpen(false);
    setCopyMaterial(null);
    setCopyName('');
    setCopySelectedOpIds([]);
    setUngroupedOps([]);
    setCopyError(null);
  };

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Управление материалами</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={onAddClick}>
          Добавить материал
        </Button>
      </Box>
      {materialsData.length === 0 && <Typography>Нет данных</Typography>}
      {materialsData.length > 0 && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Название</TableCell>
                <TableCell>Ед. изм.</TableCell>
                <TableCell>Цена</TableCell>
                <TableCell>Коэф. отход</TableCell>
                <TableCell align="right">Действия</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {materialsData.map((mat) => (
                <TableRow key={mat.id}>
                  <TableCell>{mat.name}</TableCell>
                  <TableCell>{mat.unit}</TableCell>
                  <TableCell>{mat.price?.toFixed(2)} ₽</TableCell>
                  <TableCell>{mat.wasteCoefficient?.toString()}</TableCell>
                  <TableCell align="right">
                    <IconButton size="small" onClick={() => onEditClick(mat)}><Edit /></IconButton>
                    <IconButton size="small" color="error" onClick={() => onDeleteClick(mat)}><Delete /></IconButton>
                    <Button size="small" variant="outlined" onClick={() => openOpsDialog(mat)}>
                      Операции
                    </Button>
                    <IconButton size="small" onClick={() => openCopyDialog(mat)} title="Копировать">
                      <CopyAll fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog open={opsDialogOpen} onClose={handleCloseOpsDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          Операции для материала: {opsDialogMaterial?.name}
        </DialogTitle>
        <DialogContent>
          {opsError && (
            <Alert severity="error" sx={{ mb: 2 }}>{opsError}</Alert>
          )}
          {opsLoading ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, mt: 1 }}>
              {groupedOps.length === 0 && ungroupedOps.length === 0 && !opsError && (
                <Typography color="text.secondary">Нет операций. Создайте операции и группировки в соответствующих вкладках.</Typography>
              )}
              {groupedOps.map(group => (
                <Box key={group.id} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, p: 1.5 }}>
                  <Typography variant="subtitle2" color="primary" gutterBottom>
                    {group.name}
                  </Typography>
                  <FormGroup>
                    {(group.operations || []).map(op => (
                      <FormControlLabel
                        key={op.id}
                        control={
                          <Checkbox
                            checked={selectedOpIds.includes(op.id)}
                            onChange={() => toggleOp(op.id)}
                          />
                        }
                        label={op.name + (op.price != null ? ` — ${op.price} ₽` : '')}
                      />
                    ))}
                  </FormGroup>
                </Box>
              ))}
              {ungroupedOps.filter(op => op.name).map(op => (
                <Box key={op.id} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, p: 1.5 }}>
                  <Typography variant="subtitle2" color="primary" gutterBottom>
                    Без группы
                  </Typography>
                  <FormGroup>
                    <FormControlLabel
                      control={
                        <Checkbox
                          checked={selectedOpIds.includes(op.id)}
                          onChange={() => toggleOp(op.id)}
                        />
                      }
                      label={op.name + (op.price != null ? ` — ${op.price} ₽` : '')}
                    />
                  </FormGroup>
                </Box>
              ))}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseOpsDialog}>Отмена</Button>
          <Button onClick={handleSaveOps} variant="contained" disabled={opsLoading}>
            Сохранить
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={copyDialogOpen} onClose={handleCloseCopyDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          Копировать материал: {copyMaterial?.name}
        </DialogTitle>
        <DialogContent>
          {copyError && (
            <Alert severity="error" sx={{ mb: 2 }}>{copyError}</Alert>
          )}
          <TextField
            autoFocus
            fullWidth
            margin="dense"
            label="Название нового материала"
            value={copyName}
            onChange={(e) => setCopyName(e.target.value)}
          />
          {opsLoading ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, mt: 1 }}>
              {groupedOps.length === 0 && ungroupedOps.length === 0 && !opsError && !copyError && (
                <Typography color="text.secondary">Нет операций. Создайте операции и группировки в соответствующих вкладках.</Typography>
              )}
              {groupedOps.map(group => (
                <Box key={group.id} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, p: 1.5 }}>
                  <Typography variant="subtitle2" color="primary" gutterBottom>
                    {group.name}
                  </Typography>
                  <FormGroup>
                    {(group.operations || []).map(op => (
                      <FormControlLabel
                        key={op.id}
                        control={
                          <Checkbox
                            checked={copySelectedOpIds.includes(op.id)}
                            onChange={() => toggleOpCopy(op.id)}
                          />
                        }
                        label={op.name + (op.price != null ? ` — ${op.price} ₽` : '')}
                      />
                    ))}
                  </FormGroup>
                </Box>
              ))}
              {ungroupedOps.filter(op => op.name).map(op => (
                <Box key={op.id} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, p: 1.5 }}>
                  <Typography variant="subtitle2" color="primary" gutterBottom>
                    Без группы
                  </Typography>
                  <FormGroup>
                    <FormControlLabel
                      control={
                        <Checkbox
                          checked={copySelectedOpIds.includes(op.id)}
                          onChange={() => toggleOpCopy(op.id)}
                        />
                      }
                      label={op.name + (op.price != null ? ` — ${op.price} ₽` : '')}
                    />
                  </FormGroup>
                </Box>
              ))}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseCopyDialog}>Отмена</Button>
          <Button onClick={handleCreateCopy} variant="contained" disabled={copyLoading || !copyName.trim()}>
            Создать
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default MaterialsTab;
