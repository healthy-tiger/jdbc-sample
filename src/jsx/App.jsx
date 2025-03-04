import '../css/template.css';
import '../css/index.css';
import { useState, useRef, StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { AgGridReact } from 'ag-grid-react';
import { ModuleRegistry, AllCommunityModule, } from 'ag-grid-community';
ModuleRegistry.registerModules([AllCommunityModule,]);

const GridExample = () => {
    const [coldefs] = useState([
        { field: '0', headerName: 'データセットID', },
        { field: '1', headerName: 'データセットタイトル', },
        { field: '2', headerName: 'データセット名称', },
        { field: '3', headerName: '公表組織名', },
        { field: '4', headerName: '作成者', },
        { field: '5', headerName: 'グループタイトル', },
        { field: '6', headerName: '作成頻度', },
        { field: '7', headerName: '説明', },
        { field: '8', headerName: 'リリース日', },
    ]);
    const [rows, setRows] = useState([]);
    const [count, setCount] = useState(0);
    const dialog = useRef(null);
    const controller = useRef(null);
    const [title, setTitle] = useState(null);
    const decoder = useRef(new TextDecoder());
    const sizeofint = 4;
    const rawrecords = useRef([]);

    const doSearch = async (evt) => {
        evt.preventDefault();

        dialog.current.showModal();

        const params = new URLSearchParams();
        params.set('title', title);

        controller.current = new AbortController();
        try {
            const response = await fetch(`webapi/oddataset?${params}`, { signal: controller.current.signal });
            if (!response.ok) {
                throw new Error(`Response status: ${response.status}`);
            }
            const reader = response.body.getReader({mode:'byob'});
            let lengthview = new DataView(new ArrayBuffer(sizeofint));
            let chunk = await reader.read(lengthview, {min:sizeofint});
            while (!chunk.done) {
                const len = chunk.value.getInt32(0, false);
                //console.log(len);
                const textview = new DataView(new ArrayBuffer(len));
                chunk = await reader.read(textview, {min:len});
                const s = decoder.current.decode(chunk.value);
                //console.log(s);
                try {
                    const r = JSON.parse(s);
                    rawrecords.current.push(r.r);
                    setCount(rawrecords.current.length);
                }
                catch(e) {
                    console.error(e);
                }
                lengthview = new DataView(new ArrayBuffer(sizeofint));
                chunk = await reader.read(lengthview, {min:sizeofint});
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

