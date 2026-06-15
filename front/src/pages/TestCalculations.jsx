import { useState, useEffect } from 'react';
import api from '../services/api';
import MaterialSelector from '../components/MaterialSelector';
import DimensionsInput from '../components/DimensionsInput';
import OperationsSelector from '../components/OperationsSelector';
import CalculationResult from '../components/CalculationResult';

const TestCalculations = () => {
  const [materials, setMaterials] = useState([]);
  const [operations, setOperations] = useState([]);
  const [eyelets, setEyelets] = useState([]);
  const [selectedMaterial, setSelectedMaterial] = useState(null);
  const [dimensions, setDimensions] = useState({ widthM: '', heightM: '' });
  const [selectedOperations, setSelectedOperations] = useState([]);
  const [podvorot, setPodvorot] = useState({ mmHorizontal: '', mmVertical: '', countPerSide: 2 });
  const [eyeletSettings, setEyeletSettings] = useState({ selectedEyelet: null, stepCm: 40 });
  const [calculationResult, setCalculationResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [loadingOperations, setLoadingOperations] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchMaterials();
    fetchOperations();
    fetchEyelets();
  }, []);

  const fetchMaterials = async () => {
    try {
      const { data } = await api.get('/api/v1/calculations/materials');
      setMaterials(data);
    } catch (error) {
      console.error('Error fetching materials:', error);
      setError('Failed to load materials. Please try again.');
    }
  };

  const fetchOperations = async () => {
    setLoadingOperations(true);
    try {
      const { data } = await api.get('/api/v1/calculations/operations');
      setOperations(data || []);
    } catch (error) {
      console.error('Error fetching operations:', error);
      setError('Failed to load operations. Please try again.');
    } finally {
      setLoadingOperations(false);
    }
  };

  const fetchEyelets = async () => {
    try {
      const { data } = await api.get('/api/v1/calculations/eyelets');
      setEyelets(data || []);
    } catch (error) {
      console.error('Error fetching eyelets:', error);
      setError('Failed to load eyelets. Please try again.');
    }
  };

  const handleCalculate = async () => {
    setError(null);

    if (!selectedMaterial) {
      setError('Please select a material');
      return;
    }

    const width = parseFloat(dimensions.widthM);
    const height = parseFloat(dimensions.heightM);
    if (!dimensions.widthM || isNaN(width) || width <= 0) {
      setError('Width must be a positive number');
      return;
    }
    if (!dimensions.heightM || isNaN(height) || height <= 0) {
      setError('Height must be a positive number');
      return;
    }

    if (selectedMaterial.type === 'BANNER') {
      const horiz = parseFloat(podvorot.mmHorizontal);
      const vert = parseFloat(podvorot.mmVertical);
      if (podvorot.mmHorizontal && (isNaN(horiz) || horiz < 0)) {
        setError('Horizontal podvorot must be a non-negative number');
        return;
      }
      if (podvorot.mmVertical && (isNaN(vert) || vert < 0)) {
        setError('Vertical podvorot must be a non-negative number');
        return;
      }
      if (eyeletSettings.stepCm && (isNaN(parseInt(eyeletSettings.stepCm)) || parseInt(eyeletSettings.stepCm) <= 0)) {
        setError('Eyelet step must be a positive number');
        return;
      }
    }

    setLoading(true);
    const requestData = {
      materialId: selectedMaterial.id,
      materialType: selectedMaterial.type,
      widthM: parseFloat(dimensions.widthM),
      heightM: parseFloat(dimensions.heightM),
      operationIds: selectedOperations.map(op => op.id),
      podvorotMmHorizontal: selectedMaterial.type === 'BANNER' ? parseFloat(podvorot.mmHorizontal) || null : null,
      podvorotMmVertical: selectedMaterial.type === 'BANNER' ? parseFloat(podvorot.mmVertical) || null : null,
      podvorotCountPerSide: podvorot.countPerSide,
      eyeletId: selectedMaterial.type === 'BANNER' && eyeletSettings.selectedEyelet ? eyeletSettings.selectedEyelet.id : null,
      eyeletStepCm: eyeletSettings.stepCm,
    };

    try {
      const { data } = await api.post('/api/v1/calculations', requestData);
      setCalculationResult(data);
    } catch (error) {
      console.error('Error calculating:', error);
      setError('Failed to calculate. Please check your inputs and try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">Test Calculations</h1>
      {error && <p className="text-red-500 mb-4">{error}</p>}
      <MaterialSelector
        materials={materials}
        selectedMaterial={selectedMaterial}
        onSelect={setSelectedMaterial}
      />
      <DimensionsInput
        dimensions={dimensions}
        onChange={setDimensions}
      />
      {selectedMaterial?.type === 'BANNER' && (
        <>
          <div className="mb-4">
            <label className="block text-sm font-medium mb-2">Podvorot (mm)</label>
            <div className="flex space-x-2">
              <input
                type="number"
                placeholder="Horizontal (mm)"
                value={podvorot.mmHorizontal}
                onChange={(e) => setPodvorot({ ...podvorot, mmHorizontal: e.target.value })}
                className="border p-2 flex-1"
              />
              <input
                type="number"
                placeholder="Vertical (mm)"
                value={podvorot.mmVertical}
                onChange={(e) => setPodvorot({ ...podvorot, mmVertical: e.target.value })}
                className="border p-2 flex-1"
              />
              <input
                type="number"
                placeholder="Count per side"
                value={podvorot.countPerSide}
                onChange={(e) => setPodvorot({ ...podvorot, countPerSide: parseInt(e.target.value) || 2 })}
                className="border p-2 w-24"
              />
            </div>
          </div>
          <div className="mb-4">
            <label className="block text-sm font-medium mb-2">Eyelets</label>
            <select
              value={eyeletSettings.selectedEyelet?.id || ''}
              onChange={(e) => {
                const eyelet = eyelets.find(ey => ey.id === parseInt(e.target.value));
                setEyeletSettings({ ...eyeletSettings, selectedEyelet: eyelet });
              }}
              className="border p-2 w-full mb-2"
            >
              <option value="">Choose eyelet</option>
              {eyelets.map(eyelet => (
                <option key={eyelet.id} value={eyelet.id}>
                  {eyelet.name} - {eyelet.pricePerPiece} руб, {eyelet.diameterMm} mm
                </option>
              ))}
            </select>
            <input
              type="number"
              placeholder="Step (cm)"
              value={eyeletSettings.stepCm}
              onChange={(e) => setEyeletSettings({ ...eyeletSettings, stepCm: parseInt(e.target.value) || 40 })}
              className="border p-2 w-full"
            />
          </div>
        </>
      )}
      <OperationsSelector
        operations={operations}
        selectedOperations={selectedOperations}
        onSelect={setSelectedOperations}
        materialType={selectedMaterial?.type}
        loading={loadingOperations}
      />
      <button
        onClick={handleCalculate}
        disabled={loading}
        className="bg-blue-500 text-white px-4 py-2 rounded mt-4"
      >
        {loading ? 'Calculating...' : 'Calculate'}
      </button>
      {calculationResult && (
        <CalculationResult result={calculationResult} />
      )}
    </div>
  );
};

export default TestCalculations;