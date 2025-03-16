import '../css/template.css';
import '../css/index.css';
import { useState, useRef, StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { AgGridReact } from 'ag-grid-react';
import { ModuleRegistry, AllCommunityModule, } from 'ag-grid-community';
import read from './jsonstream';
import { parseDate } from './day';
ModuleRegistry.registerModules([AllCommunityModule,]);

const getColumnValue = params => {
    const value = params.data.columns[params.colDef.field];
    if(params.colDef.field == '8') {
        if(value == null || null == parseDate(value)) {
            params.data.invalidCols.add(params.colDef.field);
        } else {
            params.data.invalidCols.delete(params.colDef.field);
        }
    }
    return value;
}

const setColumnValue = params => {
   params.data.columns[params.colDef.field] = params.newValue;
    return true;
}

const cellStyleCb = params => {
    if(params.data.invalidCols.has(params.colDef.field)) {
        return { backgroundColor: 'salmon' }
    } else {
        return { backgroundColor: 'white' }
    }
}

const GridExample = () => {
    const [coldefs] = useState([
        { field: '0', editable: false, cellStyle: cellStyleCb, valueGetter: getColumnValue, valueSetter: setColumnValue, headerName: 'データセットID', },
        { field: '1', editable: true,  cellStyle: cellStyleCb, valueGetter: getColumnValue, valueSetter: setColumnValue, headerName: 'データセットタイトル', },
        { field: '2', editable: true,  cellStyle: cellStyleCb, valueGetter: getColumnValue, valueSetter: setColumnValue, headerName: 'データセット名称', },
        { field: '3', editable: true,  cellStyle: cellStyleCb, valueGetter: getColumnValue, valueSetter: setColumnValue, headerName: '公表組織名', },
        { field: '4', editable: true,  cellStyle: cellStyleCb, valueGetter: getColumnValue, valueSetter: setColumnValue, headerName: '作成者', },
        { field: '5', editable: true,  cellStyle: cellStyleCb, valueGetter: getColumnValue, valueSetter: setColumnValue, headerName: 'グループタイトル', },
        { field: '6', editable: true,  cellStyle: cellStyleCb, valueGetter: getColumnValue, valueSetter: setColumnValue, headerName: '作成頻度', },
        { field: '7', editable: true,  cellStyle: cellStyleCb, valueGetter: getColumnValue, valueSetter: setColumnValue, headerName: '説明', },
        { field: '8', editable: true,  cellStyle: cellStyleCb, valueGetter: getColumnValue, valueSetter: setColumnValue, headerName: 'リリース日', },
    ]);
    const [rows, setRows] = useState([]);
    const [count, setCount] = useState(0);
    const dialog = useRef(null);
    const controller = useRef(null);
    const [title, setTitle] = useState(null);
    const rawrecords = useRef([]);

    const doSearch = async (evt) => {
        evt.preventDefault();

        dialog.current.showModal();

        const params = new URLSearchParams();
        params.set('title', title == null ? '' : title);

        controller.current = new AbortController();
        try {
            const response = await fetch(`webapi/oddataset?${params}`, { signal: controller.current.signal });
            if (!response.ok) {
                throw new Error(`Response status: ${response.status}`);
            }
            const reader = read(response.body.getReader());
            for await (const raw of reader) {
                rawrecords.current.push({ invalidCols: new Set(), columns: raw.r });
                setCount(rawrecords.current.length);
            }
            setRows(rawrecords.current);
            rawrecords.current = [];
        }
        catch (reason) {
            if(reason.name === 'AbortError') {
                setRows(rawrecords.current);
                rawrecords.current = [];
            } else {
                console.error(reason);
            }
        }
        finally {
            dialog.current.close();
        }
    }

    const doCancel = () => {
        controller.current.abort('The request was canceled by the user.');
    }

    return (
        <>
            <dialog ref={dialog} onCancel={doCancel}>
                <div>読み込み中...{count}</div>
                <div>
                    <button onClick={doCancel}>キャンセル</button>
                </div>
            </dialog>
            <div id="pos-data-container">
                <div id="pos-data-controller">
                    <form>
                        <label for="title">タイトル:</label>
                        <input type="text" value={title} onChange={e => setTitle(e.currentTarget.value)} />
                        <button onClick={doSearch}>検索</button>
                    </form>
                </div>
                <div id="pos-data-grid">
                    <AgGridReact columnDefs={coldefs} rowData={rows} />
                </div>
                <div id="pos-data-status">
                    <span>{count} 件</span>
                </div>
            </div>
        </>
    );
}

const appelm = document.getElementById('app');
const contextpath = appelm.dataset['contextpath'];
const root = createRoot(appelm);
root.render(<StrictMode>
    <GridExample contextpath={contextpath} />
</StrictMode>);

