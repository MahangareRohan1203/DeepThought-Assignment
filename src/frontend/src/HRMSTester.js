import React, { useState, useEffect } from 'react';
import { Button, Input, Form, Card, Space, Typography, message, Table, Tag, Select, Divider, Collapse, InputNumber } from 'antd';
import { 
    createWorker, 
    createSite, 
    clockIn, 
    clockOut, 
    getActiveWorkers, 
    getAllWorkers,
    getAllSites
} from './client';

const { Title, Text } = Typography;
const { Option } = Select;
const { Panel } = Collapse;

const HRMSTester = () => {
    const [activeWorkers, setActiveWorkers] = useState([]);
    const [workers, setWorkers] = useState([]);
    const [sites, setSites] = useState([]);
    const [loading, setLoading] = useState(false);

    const fetchData = () => {
        setLoading(true);
        Promise.all([getAllWorkers(), getAllSites(), getActiveWorkers()])
            .then(responses => Promise.all(responses.map(res => res.json())))
            .then(([workersData, sitesData, activeData]) => {
                setWorkers(workersData);
                setSites(sitesData);
                setActiveWorkers(activeData);
            })
            .catch(err => message.error('Failed to fetch data'))
            .finally(() => setLoading(false));
    };

    useEffect(() => {
        fetchData();
    }, []);

    const onFinishWorker = (values) => {
        createWorker({
            ...values,
            dailyWageRate: parseFloat(values.dailyWageRate),
            activeStatus: true
        })
        .then(() => {
            message.success('Worker created successfully');
            fetchData();
        })
        .catch(err => message.error('Failed to create worker'));
    };

    const onFinishSite = (values) => {
        createSite({
            ...values,
            activeStatus: true
        })
        .then(() => {
            message.success('Site created successfully');
            fetchData();
        })
        .catch(err => message.error('Failed to create site'));
    };

    const onFinishClockIn = (values) => {
        clockIn({
            workerId: values.workerId,
            siteId: values.siteId
        })
        .then(() => {
            message.success('Clock-in successful');
            fetchData();
        })
        .catch(err => message.error('Failed to clock-in'));
    };

    const onFinishClockOut = (values) => {
        clockOut({
            workerId: values.workerId
        })
        .then(() => {
            message.success('Clock-out successful');
            fetchData();
        })
        .catch(err => message.error('Failed to clock-out'));
    };

    const formatTime = (isoString) => {
        if (!isoString) return "-";
        return new Date(isoString).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' });
    };

    return (
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Title level={3}>HRMS Management Dashboard (Senior Architecture)</Title>
                <Button onClick={fetchData} loading={loading}>Refresh All Data</Button>
            </div>

            <Card title="Register New Worker / Site">
                <Text strong>Worker Registration</Text>
                <Form layout="inline" onFinish={onFinishWorker} style={{ marginBottom: 15, marginTop: 5 }}>
                    <Form.Item name="name" rules={[{ required: true }]}><Input placeholder="Name" /></Form.Item>
                    <Form.Item name="phone" rules={[{ required: true }]}><Input placeholder="Phone" /></Form.Item>
                    <Form.Item name="designation" rules={[{ required: true }]}>
                        <Select placeholder="Designation" style={{ width: 120 }}>
                            <Option value="MASON">MASON</Option>
                            <Option value="ELECTRICIAN">ELECTRICIAN</Option>
                            <Option value="PLUMBER">PLUMBER</Option>
                            <Option value="SUPERVISOR">SUPERVISOR</Option>
                            <Option value="HELPER">HELPER</Option>
                        </Select>
                    </Form.Item>
                    <Form.Item name="dailyWageRate" rules={[{ required: true }]}><Input placeholder="Daily Wage" /></Form.Item>
                    <Button type="primary" htmlType="submit">Add Worker</Button>
                </Form>
                
                <Divider />
                
                <Text strong>Site Registration (with Rule Overrides)</Text>
                <Form layout="vertical" onFinish={onFinishSite} style={{ marginTop: 10 }}>
                    <Space align="start">
                        <Form.Item label="Site Name" name="siteName" rules={[{ required: true }]}><Input placeholder="Site Name" /></Form.Item>
                        <Form.Item label="Location" name="location" rules={[{ required: true }]}><Input placeholder="Location" /></Form.Item>
                        <Button type="primary" htmlType="submit" style={{ marginTop: 30 }}>Add Site</Button>
                    </Space>
                    
                    <Collapse ghost>
                        <Panel header="Advanced Business Rule Overrides (Optional)" key="1">
                            <Space size="middle">
                                <Form.Item label="Std Shift Hours" name="customStandardShiftHours">
                                    <InputNumber placeholder="e.g. 10.0" min={1} max={24} />
                                </Form.Item>
                                <Form.Item label="Monthly OT Cap" name="customMonthlyOvertimeCap">
                                    <InputNumber placeholder="e.g. 80.0" min={0} />
                                </Form.Item>
                                <Form.Item label="Max Shift (Safety)" name="customMaxShiftHours">
                                    <InputNumber placeholder="e.g. 12" min={1} max={24} />
                                </Form.Item>
                            </Space>
                            <div style={{ color: '#888', fontSize: '12px' }}>* Leave empty to use Global Company Defaults.</div>
                        </Panel>
                    </Collapse>
                </Form>
            </Card>

            <div style={{ display: 'flex', gap: '20px' }}>
                <Card title="Current Workers" style={{ flex: 1 }}>
                    <Table 
                        dataSource={workers}
                        size="small"
                        pagination={{ pageSize: 5 }}
                        columns={[
                            { title: 'Name', dataIndex: 'name' },
                            { title: 'Designation', dataIndex: 'designation' },
                            { title: 'Registered At', dataIndex: 'createdAt', render: t => formatTime(t) },
                            { title: 'Status', dataIndex: 'activeStatus', render: s => s ? <Tag color="green">Active</Tag> : <Tag color="red">Inactive</Tag> }
                        ]}
                        rowKey="id"
                    />
                </Card>
                <Card title="Available Sites" style={{ flex: 1 }}>
                    <Table 
                        dataSource={sites}
                        size="small"
                        pagination={{ pageSize: 5 }}
                        columns={[
                            { title: 'Name', dataIndex: 'siteName' },
                            { title: 'Location', dataIndex: 'location' },
                            { title: 'Rules', key: 'rules', render: (_, record) => (
                                <Space direction="vertical" size={0}>
                                    {record.customStandardShiftHours ? <Tag color="blue">Shift: {record.customStandardShiftHours}h</Tag> : <Tag>Shift: Default</Tag>}
                                    {record.customMonthlyOvertimeCap ? <Tag color="purple">Cap: {record.customMonthlyOvertimeCap}h</Tag> : <Tag>Cap: Default</Tag>}
                                </Space>
                            )}
                        ]}
                        rowKey="id"
                    />
                </Card>
            </div>

            <Card title="Real-time Attendance Control">
                <Space size="large" align="start">
                    <div style={{ border: '1px solid #f0f0f0', padding: '15px', borderRadius: '8px' }}>
                        <Title level={5}>Clock In</Title>
                        <Form layout="vertical" onFinish={onFinishClockIn}>
                            <Form.Item name="workerId" label="Select Worker" rules={[{ required: true }]}>
                                <Select style={{ width: 200 }} placeholder="Choose Worker">
                                    {workers.filter(w => !activeWorkers.find(aw => aw.workerId === w.id)).map(w => (
                                        <Option key={w.id} value={w.id}>{w.name}</Option>
                                    ))}
                                </Select>
                            </Form.Item>
                            <Form.Item name="siteId" label="Select Site" rules={[{ required: true }]}>
                                <Select style={{ width: 200 }} placeholder="Choose Site">
                                    {sites.map(s => (
                                        <Option key={s.id} value={s.id}>{s.siteName}</Option>
                                    ))}
                                </Select>
                            </Form.Item>
                            <Button type="primary" htmlType="submit" block>Confirm Clock In</Button>
                        </Form>
                    </div>

                    <div style={{ border: '1px solid #f0f0f0', padding: '15px', borderRadius: '8px' }}>
                        <Title level={5}>Clock Out</Title>
                        <Form layout="vertical" onFinish={onFinishClockOut}>
                            <Form.Item name="workerId" label="Select Active Worker" rules={[{ required: true }]}>
                                <Select style={{ width: 200 }} placeholder="Choose Worker">
                                    {activeWorkers.map(aw => (
                                        <Option key={aw.workerId} value={aw.workerId}>{aw.workerName}</Option>
                                    ))}
                                </Select>
                            </Form.Item>
                            <div style={{ height: '74px' }} />
                            <Button type="primary" danger htmlType="submit" block>Confirm Clock Out</Button>
                        </Form>
                    </div>

                    <div style={{ flex: 1 }}>
                        <Title level={5}>Live Active Workers (Redis Cache)</Title>
                        <Table 
                            dataSource={activeWorkers}
                            size="small"
                            columns={[
                                { title: 'Worker', dataIndex: 'workerName' },
                                { title: 'Site', dataIndex: 'siteName' },
                                { title: 'Clocked In At', dataIndex: 'clockInTime', render: t => new Date(t).toLocaleTimeString() },
                            ]}
                            rowKey="workerId"
                        />
                    </div>
                </Space>
            </Card>
        </Space>
    );
};

export default HRMSTester;
