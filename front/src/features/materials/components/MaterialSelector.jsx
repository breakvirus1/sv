import React, { useState, useEffect } from 'react';
import {
  Dialog, DialogTitle, DialogContent, TextField, Table, TableBody, TableCell,
  TableHead, TableRow, Button, CircularProgress, Box
} from '@mui/material';

const MaterialSelector = ({ open, onClose, onSelect }) => {
  const [search, setSearch] = useState('');
  const [materials, setMaterials] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchMaterials = async (query) => {
    setLoading(true);
    try {
      const res = await fetch(`/api/v1/materials?search=${encodeURIComponent(query)}&size=100`);
      const data = await res.json();
      // Expecting array of {id, name, price, unit}
      setMaterials(data.content || data || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (open) {
      fetchMaterials(search);
    }
  }, [open, search]);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>Выбрать материал из базы</DialogTitle>
      <DialogContent>
        <TextField
          fullWidth
          placeholder="Поиск материала..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          sx={{ mb: 2 }}
          InputProps={{
            endAdornment: loading ? <CircularProgress size={20} /> : null
          }}
        />

        <Table size="small" sx={{ minWidth: 500 }}>
          <TableHead>
            <TableRow>
              <TableCell>Наименование</TableCell>
              <TableCell align="right">Цена</TableCell>
              <TableCell>Ед.изм.</TableCell>
              <TableCell></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {materials.map(mat => (
              <TableRow key={mat.id} hover>
                <TableCell>{mat.name}</TableCell>
                <TableCell align="right">{Number(mat.price).toFixed(2)} ₽</TableCell>
                <TableCell>{mat.unit || 'шт'}</TableCell>
                <TableCell>
                  <Button variant="outlined" size="small" onClick={() => {
                    onSelect(mat);
                    onClose();
                  }}>
                    Выбрать
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </DialogContent>
    </Dialog>
  );
};

export default MaterialSelector;
